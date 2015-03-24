# Auto-synchronized Map Locations Notepad

CSI 5175 Assignment 2, University of Ottawa

Ling Jin
March 24, 2015

# Introduction

Mappad implements 3 Activities for displaying note list, editing note and show the note location in map. A Service is introduced to communicate with the Amazon Web Services Simple Storage Service server. When compose the note, users can select a location from Google Maps and input a label of that location. The note file will synchronize to S3 server automatically. A simple file synchronize procedure has been implemented to manage the synchronizations, including adding a file, deleting a file and modify a file.
