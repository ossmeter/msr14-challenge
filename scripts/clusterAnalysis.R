#!/usr/bin/Rscript
library(fpc)
library(data.table)
library(plyr)

analyse <- function(data, out, keycol) {
	# Try numerous K's to discover natural groupings
	for (k in c(2,3,4,5,6)) {
		fit <- kmeans(data, k)

		# Plot parallel coordinates of the centroids
		pdf(paste0(out,"-",k,".pdf"), width=10, height=6)
		parcoord(fit$centers, var.label=TRUE, lty=seq(1,k,1))
		legend("right",bg="white", legend = seq(1,k,1), lty=seq(1,k,1))
		dev.off()

		# Append the group to the data for future reference
		groups <- data.frame(fit$cluster)
		colnames(groups)<-c('group')

		# Commented out due to GitHub not liking large files
		# foo <- cbind(data, groups, keycol)
		# filename <- paste0(out,"-",k,".csv")
		# write.csv(foo, file=filename)

		# Print summaries of the groups
		print(paste0("for K=", k))
		for (j in seq(1,k,1)) {
			print(paste0("    group ", j, " = ", length(groups[groups$group==j,]),"/", length(groups$group)," :=> ", (length(groups[groups$group==j,])/length(groups$group))))
		}
	}

}

# ID of experiment
exp <- "gte1000"

# Create a folder for our results
out <- paste0("../results/",exp,"/")
dir.create(out)
out <- paste0(out,exp)

# Redirect output
sink(paste0("../results/",exp,"/output.txt"))

# Read data and analyse
data <- read.csv("../data/cleaneddata.csv")
data <- data[!data$user=="",]
data <- data[data$total>=1000,] # Change at will

activity <- cbind(	data$commits, 
					data$commit_comments, 
					data$issues, 
					data$issue_comments, 
					data$pull_requests, 
					data$pull_requests_comments
					)
colnames(activity) <- c('commits',
						'commit_comments',
						'issues',
						'issue_comments', 
						'pull_requests', 
						'pull_requests_comments')


analyse(activity, out, data$key)
