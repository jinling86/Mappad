# Mappad

A location notebook APP, implemented for CSI 5171 Assignment 2

Ling Jin @ uOttawa, March 24, 2015

# Features

This APP uses AWS Simple Storage Service (S3) to store notes which contain locations. The locations are pointed by a user from an embeded map. The default location is the user's current location which is obtained from the Google Map service. A user can browse all the locations stored in the notes.

A large part of the code deals with the synchronization of the note files between the cell phone and the AWS S3 server. It tries hard to delete, upload and download so as to keep the note files up-to-date. Some special situations, e.g. file creation and deletion during cell phone off-line, have been considered. It is a somehow stable APP that you can play with.

A note is separately saved in text format as required by our instructor. I think using only one file may make life easy. After all, the whole APP only worths 5 final marks... ( I can fill an entire paper with my complains :)

Use your own Amazon Web Server access key and Google API key. Make sure your cell phone or emulator supports Google Play. The instructions of how to get these keys can be found in the comments.

# Screenshots
![alt text](https://github.com/ljin027/Mappad/blob/master/app/src/main/res/drawable/ScreenShot.png)

# Todo's

Do more tests on the file synchronization procedures.

A bug: the validity of a note file is determined by a timestamp. Unfortunately, the timestamp cannot tell the timezone, which means one note created at 8 in China and the other note created at 8 in Canada will have a same name, although there are 12 hours difference. Some detriments are caused by this problem. 

# License

Feel free to use Mappad as well as its code.

Good luck and have fun!
