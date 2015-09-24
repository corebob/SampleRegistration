# SampleRegistration
Android application for sample registration in the field

The application creates a folder called "sampleregistration" under the DOCUMENTS system folder.
The sampleregistration folder contains two new folders, "projects" and "config".

The projects folder contains the log files for each project.

If there is a file called sample-types.txt under the config folder, this file will be used as a suggestion database for the sample type field.
The sample-types.txt file should have one sample type per line.

If there is a file called units.txt under the config folder, this file will be used as a suggestion database for the unit field.
The units.txt file should have one unit per line.