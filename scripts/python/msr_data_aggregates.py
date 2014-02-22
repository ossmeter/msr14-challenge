import csv

clean_file = open('clean.csv', 'r', newline='')
target_file = open('aggregates.csv', 'w', newline='')

source = csv.reader(clean_file)
target = csv.writer(target_file)

header =[
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
         ]

aggregates = [0]*14
current_project='None'
number_of_users = 0
target.writerow(header)
entries_list = list(csv.reader(clean_file))[1:]
iter= 0
for row in entries_list:                           
    if current_project == row[0]:
        aggregates= list(map(lambda x: float(x[0]) + float(x[1]), zip(aggregates, row[-14:])))
        number_of_users +=1
    else:
        if current_project =='None':
            current_project= row[0]
            aggregates= list(map(lambda x: float(x[0]) + float(x[1]), zip(aggregates, row[-14:])))
            number_of_users +=1  
        else:
            aggregates.insert(0, current_project)
            aggregates.insert(1, current_project.split(sep='_')[0])
            aggregates.insert(2, number_of_users)
            aggregates.insert(3, current_project.split(sep='_')[1])
            target.writerow(aggregates)
            current_project = row[0]
            aggregates = [0]*14
            number_of_users = 0 
            aggregates= list(map(lambda x: float(x[0]) + float(x[1]), zip(aggregates, row[-14:])))
            number_of_users +=1
            if iter == len(entries_list)-1:  
                aggregates.insert(0, current_project)
                aggregates.insert(1, current_project.split(sep='_')[0])
                aggregates.insert(2, number_of_users)
                aggregates.insert(3, current_project.split(sep='_')[1])
                target.writerow(aggregates)
    iter+=1   
       
