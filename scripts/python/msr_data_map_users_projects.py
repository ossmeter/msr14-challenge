import csv
from lab.msr_data_util import convert_to_dict

clean_file = open('clean.csv', 'r', newline='')
total_file = open('aggregates.csv', 'r', newline='')
output_file = open('mappeddata.csv', 'w',  newline='')

clean = csv.reader(clean_file)
total = csv.reader(total_file)
output= csv.writer(output_file)
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
         'collaborator'
         'total_users'
         ]

output.writerow(header)
total_dict = convert_to_dict(list(csv.reader(total_file))[1:])
for row in list(clean)[1:]:
    row.append(total_dict[row[0]][2])
    output.writerow(row)



#Housekeeping
clean_file.close()
total_file.close()
output_file.close()