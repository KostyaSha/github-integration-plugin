import static org.jenkinsci.plugins.github.pullrequest.utils.JobHelper.ghPRTriggerFromJob
import static com.github.kostyasha.github.integration.branch.utils.JobHelper.ghBranchTriggerFromJob

import com.github.kostyasha.github.integration.branch.GitHubBranchTrigger
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger

Jenkins.instance.getAllItems(Job.class).each{ job ->
  GitHubPRTrigger prTrigger = ghPRTriggerFromJob(job)
  if (prTrigger != null) {
    println "Starting GH PR trigger for " + job.getFullName()
    try {
    	prTrigger.start(job, true)
    } catch (Throwable error) {
    	println "ERROR: failed to start branch trigger for " + job.getFullName()
    	print error   
    }
  }
  
  GitHubBranchTrigger branchTrigger = ghBranchTriggerFromJob(job)
  if (branchTrigger != null) {
    println "Starting GH Branch trigger for " + job.getFullName()
    try {
    	branchTrigger.start(job, true)
    } catch (Throwable error) {
      println "ERROR: failed to start branch trigger for " + job.getFullName()
      print error
           
    }
  }
}
println ""
