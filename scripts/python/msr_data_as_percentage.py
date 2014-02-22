import csv
from lab.msr_data_util import *

clean_file = open('clean.csv', 'r', newline='')
total_file = open('aggregates.csv', 'r', newline='')
mapped_file = open('mappeddata.csv', 'r',  newline='')
output_file = open('dataaspercent.csv', 'w',  newline='')

clean = csv.reader(clean_file)
total = csv.reader(total_file)
mapped= csv.reader(mapped_file)
output =csv.writer(output_file)
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
         'collaborator',
         'total']

output.writerow(header)
total_dict = convert_to_dict(list(csv.reader(total_file))[1:])

for row in list(mapped)[1:]:
    zip_list = (list(zip(row[-15:], total_dict[row[0]][-14:])))
    new_row = [ float(x)/float(y) if float(y) !=0.0 else 0 for x, y in zip_list] 
    new_row.insert(0,row[0])
    new_row.insert(1,row[1])
    new_row.insert(2,row[2])
    new_row.insert(3, row[3])
    new_row.insert(18, row[18])
    output.writerow(new_row)


#Housekeeping
clean_file.close()
total_file.close()
mapped_file.close()
output_file.close()