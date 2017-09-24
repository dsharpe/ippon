# ippon

To run this demo locally, create the following entries in your /etc/hosts file:
* 127.0.0.1	ippon.books.com
* 127.0.0.1	ippon.magazines.com

This is modified from a spring example called "gs-routing-and-filtering-complete".

Import the projects into your IDE as a maven project. Gradle has not been tested. Each maven project runs on a different port.

The service projects are "book" and "magazine" and the Zuul gateway project is "gateway".

# Usage

Run the application class in each project

Try the following URLs:
* http://ippon.books.com:8080/available
* http://ippon.books.com:8080/checked-out 
* http://ippon.magazines.com:8080/new
* http://ippon.magazines.com:8080/old
