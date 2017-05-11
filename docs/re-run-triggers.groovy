import static org.jenkinsci.plugins.github.pullrequest.utils.JobHelper.ghPRTriggerFromJob
import static com.github.kostyasha.github.integration.branch.utils.JobHelper.ghBranchTriggerFromJob

import com.github.kostyasha.github.integration.branch.GitHubBranchTrigger
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger

Jenkins.instance.getAllItems(Job.class).each{ job ->
  GitHubPRTrigger prTrigger = ghPRTriggerFromJob(job)
  if (prTrigger != null) {
    println "Running GH PR trigger for " + job.getFullName()
    try {
    	prTrigger.run()
    } catch (Throwable error) {
    	println "ERROR: failed to run branch trigger for " + job.getFullName()
    	print error   
    }
  }
  
  GitHubBranchTrigger branchTrigger = ghBranchTriggerFromJob(job)
  if (branchTrigger != null) {
    println "Running GH Branch trigger for " + job.getFullName()
    try {
    	branchTrigger.run()
    } catch (Throwable error) {
      println "ERROR: failed to run branch trigger for " + job.getFullName()
      print error
           
    }
  }
}
println ""
