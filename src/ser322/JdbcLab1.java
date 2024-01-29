package ser322;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class JdbcLab1 {
    private final String[] args;
    private String filename;
    private ResultSet rs = null;
    private ResultSetMetaData rsMeta = null;
    private Statement stmt = null;
    private PreparedStatement prepStmt = null;
    private Connection conn = null;

    public JdbcLab1(String[] args) {
        this.args = args;
    }

    public static void main(String[] args) {
        new JdbcLab1(args).run();
    }

    private void run(){
        try {
            if (args.length < 5) {
                throw new IllegalArgumentException("<queryMethod>");
            }

            String _url = args[0];
            String username = args[1];
            String password = args[2];
            String driver = args[3];

            String query = args[4];

            Class.forName(driver);

            conn = DriverManager.getConnection(_url, username, password);
            conn.setAutoCommit(false);

            switch (query) {
                case "query1" -> query1();
                case "query2" -> query2();
                case "dml1" -> dml1();
                case "export" -> export();
            }
        } catch (IllegalArgumentException ex) {
            System.out.println("USAGE: java ser322.JdbcLab1 <url> <user> <passwd> <driver> " +
                    ex.getMessage());
        }
        catch (SQLIntegrityConstraintViolationException ex) {
            System.out.println("Update FAILED: " +
                    ex.getMessage() + " already exist");
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        finally {
            try {
                if (rs != null)
                    rs.close();
                if (prepStmt != null)
                    prepStmt.close();
                if (stmt != null)
                    stmt.close();
                if (conn != null) {
                    conn.rollback();
                    conn.close();
                }
                System.out.println("SUCCESS: resources closed");

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Exports everything pulled from database into xml file
     * @throws Exception
     */
    private void export() throws Exception {
        if (args.length != 6)
            throw new IllegalArgumentException("export <filename>");

        filename = args[5];

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        // root elements
        Document doc = builder.newDocument();
        Element rootElement = doc.createElement("JdbcLab");
        doc.appendChild(rootElement);

        try {
            mapDataToXML(doc, rootElement);
        } catch (Exception ex) {
            System.out.println("Could not map all data.");
            throw ex;
        }

        try (FileOutputStream output = new FileOutputStream(filename)) {
            transformXML(doc, output);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * reads through each table and creates xml elements
     * @param doc
     * @param rootElement
     * @throws Exception
     */
    private void mapDataToXML(Document doc, Element rootElement)
            throws Exception {

        stmt = conn.createStatement();
        rs = stmt.executeQuery("show tables;");

        List<String> tableList = new ArrayList<String>();

        while (rs.next())
            tableList.add(rs.getString(1));

        if (rs != null) rs.close();
        if (stmt != null) stmt.close();

        for (String table : tableList) {
            Element tableContainer = doc.createElement(table + "s");
            rootElement.appendChild(tableContainer);
            stmt = conn.createStatement();
            rs = stmt.executeQuery("select * from " + table);

            ResultSetMetaData rsmd = rs.getMetaData();
            int numCol = rsmd.getColumnCount();
            while (rs.next()) {
                Element tupleContainer = doc.createElement(table);

                // sets each table tuple with primary key for attribute
                tupleContainer.setAttribute(rsmd.getColumnLabel(1).toLowerCase(), rs.getObject(1).toString());

                for (int i = 2; i <= numCol; i++) {
                    // gets the objcolumn name and object
                    String objCol = rsmd.getColumnLabel(i);
                    Object obj = rs.getObject(i);

                    if (objCol.toLowerCase().equals("made_by")){
                        tupleContainer.setAttribute("made_by", obj.toString());
                    }
                    else {
                        Element entryContainer = doc.createElement(objCol.toLowerCase());
                        if (obj != null) entryContainer.setTextContent(obj.toString());
                        tupleContainer.appendChild(entryContainer);
                        tableContainer.appendChild(tupleContainer);
                    }
                }
            }

            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
        }

    }

    /**
     * converts document type into xml document
     * @param doc
     * @param output
     * @throws TransformerException
     */
    private static void transformXML(Document doc, OutputStream output)
            throws TransformerException {

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();

        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(output);

        transformer.transform(source, result);
    }

    /**
     * communicates with datbase to insert customer
     * @throws Exception
     */
    private void dml1() throws Exception{
        if (args.length != 9)
            throw new IllegalArgumentException("dml1 <customer id> <product id> <name> <quantity>");

        Integer quantity = null;

        int custid = Integer.parseInt(args[5]);
        int pid = Integer.parseInt(args[6]);
        String name =  args[7].toLowerCase().equals("null") ?
                null : args[7];

        if (!args[8].toLowerCase().equals("null")) {
            quantity = Integer.parseInt(args[8]);
        }

        prepStmt = conn.prepareStatement("insert into customer " +
                "values(?, ?, ?, ?)");

        prepStmt.setInt(1, custid);
        prepStmt.setInt(2, pid);

        if (name != null)
            prepStmt.setString(3, name);
        else
            prepStmt.setNull(3, Types.NULL);

        if (quantity != null)
            prepStmt.setInt(4, quantity);
        else
            prepStmt.setNull(4, Types.NULL);

        try {
            if (prepStmt.executeUpdate() > 0)
                System.out.println("Query SUCCESS, 1 customer added");
        } catch (SQLIntegrityConstraintViolationException ex) {
            throw new SQLIntegrityConstraintViolationException("CustomerId");
        }

        conn.commit();
    }

    /**
     * gets the customer list who have purchased from a given dept
     * @throws Exception
     */
    private void query2() throws Exception{
        if (args.length != 6)
            throw new IllegalArgumentException("query2 <deptNo>");

        String deptNo = args[5];

        String stmt1 = " select name, descrip, price, quantity " +
                "from product p, customer c, dept d " +
                "where d.deptno=p.made_by and p.prodid=c.pid and deptno=?;";

        prepStmt = conn.prepareStatement(stmt1);
        prepStmt.setString(1, deptNo);
        rs = prepStmt.executeQuery();

        while (rs.next()) {
            System.out.print(rs.getString("name") + "\t");
            System.out.print(rs.getString("descrip") + "\t");
            System.out.println(rs.getFloat("price") *
                    (float) rs.getInt("quantity") );
        }
    }

    /**
     * gets the list of employees
     * @throws Exception
     */
    private void query1() throws Exception {
        stmt = conn.createStatement();

        rs = stmt.executeQuery("select EMPNO, ENAME, DNAME " +
                "from emp e, dept d " +
                "where e.deptno=d.deptno;");


        while (rs.next()) {
            System.out.print(rs.getInt("EMPNO") + "\t");
            System.out.print(rs.getString("ENAME"));
            System.out.println(rs.getString("DNAME") + "\t");
        }
    }
}
