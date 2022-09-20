## file-synchronisation
Create a local Dropbox/G-Drive like server-client architechture. The project is a prototype for file storage architecture like G-Drive or One-Drive. There are one server, two Clinets. Each clients can add/edit/delete files and server and other clients will get synchronised in real time. Each client observes it's local directory tree where the files are stored, once found, it sends that update to server. Server maintains a buffer of all the updates and when another client requests for previous updates made on the server(client polls the server in 20 secs interval), the server responses with the update and those updates are made on the other clients.
* Delta-Sync: The most important part of the project is delta sync. When an user update a text file of size >2MB. The client doesn't need to send the whole file to the server, it can only send the changed blocks(each file is devided into 2MB blocks) to server and same process is following from server to other clients.
* File is transferred over UDP and not TCP.

Clone the repo
   ```sh
   git clone https://github.com/SagnikHajra/file-synchronisation.git
   ```

Dependecies: Maven is needed.

Steps. Change the $LOCAL_DIRS in the  src/main/java/Constants.java with your local dirs where you want the local directories to be.