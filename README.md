# SonicQR-Mobile

## Table of contents
* [Project Description](#Project-Decription)
* [Setup](#Setup)
* [Technologies](#Technologies)

## Project Description
A static react web application called SonicQR-Web and an android application called SonicQR-Mobile provides the conversion of files through a series of QR codes that will flash on the sender’s web browser and for recording the QR codes to receive the file on the receiver’s mobile device respectively. To ensure that the QR codes are received by the receiver while preventing multiple loops from occurring, the sender will wait for acknowledgement from the receiver via the transmission of sound when the receiver successfully receives the frame before changing to the next frame.

[![SonicQR Video](http://img.youtube.com/vi/FloDsSYSAqo/0.jpg)](http://www.youtube.com/watch?v=FloDsSYSAqo)


## Setup
To view the webpage, it is already hosted on github
- https://rexxarang.github.io/SonicQR-Web 

Pre-requisites
- Install Android Studio

To compile and run the source code directly
- Clone the project, or download the zip file to a location of your choice. 
- Unzip the project folder.
- Open the project folder in Android Studio
- Build the project
- Connect an android device with USB Debugging enabled
  - If needed, install the USB Driver specific for the mobile device
- Run the application in android studio

## Technologies
Project is created with:
* kotlin: 1.7.10
* mlkit.barcode-scanning: 17.0.2
