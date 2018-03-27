package org.jenkinsci.plugins.github.pullrequest.utils;

import org.kohsuke.github.GHUser;
import org.kohsuke.github.GHPullRequestReviewState;

/**
 * Save the state of the given Pull request event in order to keep track of the approval states of the reviews.
 * @author Nicola Covallero
 */
public class ReviewState {
    private String reviewer;
    private String email;
    private String state;
    
    public ReviewState() {
        this.state = "pending";
        this.reviewer = null;
    }

    public ReviewState(String user, String email) {
        this.state = "pending";
        this.reviewer = user;
        this.email = email; 
    }

    public void setReviewer(String reviewer){
        this.reviewer = reviewer;
    } 

    public void setEmail(String email){
        this.email = email;
    }

    public void setState(String state){
        this.state = state;
    }

    public String getReviewer(){
        return this.reviewer;
    }

    public String getEmail(){
        return this.email;
    }

    public String getState(){
        return this.state;
    }
    

}
