# Team Elrond: Touché Task-1 2021 @ CLEF
This is the repository for the Elrond group composed by students of University of Padua enrolled in the Information Retrival course in 2021 that are partecipanting in the Clef Touché Task1 competition.

##How to use
1. Create a copy of the `example.properties` file naming it `data.properties` and update the content with your setup
2. Execute the Main passing as an argument the name of the approach to run.
Possible values are: `SimpleRun`, `KRun`, `TaskBodyRun`, `OpenNlpRun`.
   
There are some additional option needed for running the program on `tira.io` for submission, but for local running we suggest only using the values inside `data.properties`.

The additional options are:
1. `-i` for replacing the documents input directory
2. `-o` for coping the run file inside a different folder and naming it 'run.txt'