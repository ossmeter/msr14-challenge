import csv

clean_file = open('clean.csv', 'w', newline='')
fork_file = open('fork.csv', 'w', newline='')
data_file = open('data.csv', 'r',  newline='')

clean = csv.writer(clean_file)
fork = csv.writer(fork_file)
header= [
         'key',
         'project',
         'user', 
         'project_owner',
         'issues', 
         'issue_comments', 
         'commits', 
         'commit_comments',
         'commit_total_changes', 
         'commit_additions', 
         'commit_deletions',
         'commit_as_author',
         'commit_total_files',
         'average_files_per_commit',
         'pull_requests', 
         'pull_requests_comments', 
         'owner', 
         'collaborator']
fork.writerow(header)
clean.writerow(header)


clean_records = 0
fork_records=0
for row in csv.reader(data_file):
    #check if the record contains something other than zeros
    if (str(0) in row[4:15])  and (len(set(row[4:15])) == 1):
        fork.writerow(row)
        fork_records+=1
    else:
        clean.writerow(row)
        clean_records+=1

#measure unique projects
clean_project_count=0
fork_project_count=0
current_project=' '
for row in csv.reader(open('clean.csv', 'r', newline='')):
    if row[0] != current_project:
        current_project = row[0]
        clean_project_count+=1
        
for row in csv.reader(open('fork.csv', 'r', newline='')):
    if row[0] != current_project:
        current_project = row[0]
        fork_project_count+=1

print('Fork Records: '+str(fork_records))
print('Clean Records: '+str(clean_records))
print(clean_project_count)
print(fork_project_count)


#Housekeeping
clean_file.close()
fork_file.close()