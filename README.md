MSR14 Mining Challenge
===============

This project hosts the data and scripts related to the following paper submitted to MSR 2014's Mining Challenge track (http://2014.msrconf.org/challenge.php):

Nicholas Matragkas, James R. Williams, Dimitris S. Kolovos, _Analysing the 'Biodiversity' of Open Source Ecosystems: The GitHub Case_

The project is structured as follows:

* `data` : Contains a cleaned CSV of the project memberships collection of our Biodiversity database. A `mongodump` of the biodiversity database produced by the analysis can be found [here](https://www.dropbox.com/s/hjw619vfqd4z36u/msr14-biodiversity-dump.zip).
* `preprint` : Contains the authors' preprint of the paper.
* `results` : The output folder for when the script runs. Contains some sample data for projects of differing numbers of user. Please note: k-means clustering is non-deterministic [[1](#CAref)] and is not guaranteed to converge on a single solution. The plots here may not look exactly the same as those in the submitted MSR paper, however they will be very similar.
* `scripts` : Contains the R script used to apply k-means clustering to the data, some Python scripts used to clean the data, and a Java project that transforms the MSR GHTorrent dataset into the biodiversity schema. Depends on [Pongo](https://code.google.com/p/pongo). 


[[1](id:CAref)] P.-N. Tan, M. Steinbach, and V. Kumar. Introduction to Data Mining, (First Edition). Addison-Wesley Longman Publishing Co., Inc., Boston, MA, USA, 2005.