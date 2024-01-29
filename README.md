This project pulls data from a sql server and uses jdbc to run queries as well as xpath.

**To compile JdbcLab1**
```
javac -d classes -cp classes ser322.JdbcLab1 
```
Running queries are as follows:

```
java -cp <driver location>;classes ser322.JdbcLab1 
<sql url> <username> <password> <driver> <function> <additional args>...
```
For example to run the first query:
```
java -cp lib\mysql-connector-java-8.0.28.jar;classes ser322.JdbcLab1 
"jdbc:mysql://localhost:3306/jdbclab?autoReconnect=true&useSSL=false" 
root root com.mysql.cj.jdbc.Driver query1
```
* query1: prints a list of employees with their employee number, name, and department.
* query2: given a dept number, prints a list of customers who bought products from them with their total price.
* dml1: function to insert a customer into the sql database.
* export: generates an xml file with choice of placement and name.

**To compile JdbcLab2**
```
javac -d classes -cp classes ser322.JdbcLab2 
```
Running JdbcLab2
```
java -cp classes ser322.JdbcLab2 <DeptNo>
```
Example
```
java -cp classes ser322.JdbcLab2 10
```
* This takes in a dept number and prints a list of all their products.
* Uses xpath. XML file must be created using export in JdbcLab1 with destination to the following:
```
xmlfiles/completeData.xml
```