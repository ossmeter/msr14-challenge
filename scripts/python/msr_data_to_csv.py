#This script gets the data from the project_membership db and puts then in an csv file
import csv, time,  operator
from pymongo import MongoClient, ASCENDING

client = MongoClient("localhost", 27017)
db = client.biodiversity
collection_project_memberships = db.projectMemberships

output_file = open('data.csv', 'w', newline='')
data = csv.writer(output_file)

# Keep track of execution time
start = time.clock()
for project_membership in collection_project_memberships.find():     
    # store only data relevant to the analysis
    data.writerow([ 
               str(project_membership.get('projectName')+'_'+project_membership.get('projectOwner')).strip(),   
               project_membership.get('projectName'),
               project_membership.get('userName'),
               project_membership.get('projectOwner'), 
               project_membership.get('numberOfIssues') if  project_membership.get('numberOfIssues') else 0,
               project_membership.get('numberOfIssueComments') if project_membership.get('numberOfIssueComments') else 0, 
               project_membership.get('commitCount') if project_membership.get('commitCount') else 0,
               project_membership.get('numberOfCommitComments') if  project_membership.get('numberOfCommitComments') else 0,
               project_membership.get('commitTotalChanges') if project_membership.get('commitTotalChanges') else 0,
               project_membership.get('commitAdditions') if project_membership.get('commitAdditions') else 0,
               project_membership.get('commitDeletions') if project_membership.get('commitDeletions') else 0,
               project_membership.get('commitsAsAuthor') if project_membership.get('commitsAsAuthor') else 0,
               project_membership.get('commitTotalFiles') if project_membership.get('commitTotalFiles') else 0,
               project_membership.get('averageFilesPerCommit') if project_membership.get('averageFilesPerCommit') else 0,
               project_membership.get('numberOfPullRequests') if project_membership.get('numberOfPullRequests') else 0, 
               project_membership.get('numberOfPullRequestComments') if project_membership.get('numberOfPullRequestComments') else 0, 
               1 if project_membership.get('owner') else 0,
               1 if project_membership.get('collaborator') else 0
               ])


output_file.close()

#sort csv file according to project name
data = csv.reader(open('data.csv'))
sortedlist = sorted(data, key=operator.itemgetter(0))   
fileWriter = csv.writer(open('data.csv', 'w'), delimiter=',')

for row in sortedlist:
    fileWriter.writerow(row)


#Housekeeping
end = time.clock()
print('Elapsed Time: '+str(end-start))