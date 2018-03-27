package org.jenkinsci.plugins.github.pullrequest.utils;

import org.jenkinsci.plugins.github.pullrequest.utils.ReviewState;
import java.util.List;
import java.util.ArrayList;

/**
 * Save the state of the given Pull request event in order to keep track of the approval states of the reviews.
 * @author Nicola Covallero
 */
public class PRApprovalState {
    private String action;// this variable includes some info regarding the reason of the last change to the PR.
    private List<ReviewState> reviews_states;
    
    
    public PRApprovalState() {
        this.reviews_states = new ArrayList<ReviewState>();
    }

    public void setAction(String action){
        this.action = action;
    }

    public void setReviews_states (List<ReviewState> reviews_states){
        this.reviews_states = reviews_states;
    } 

    public List<ReviewState> getReviews_states(){
        return this.reviews_states;
    }

    public String getAction(){
        return this.action;
    }

    public void addReviewState(ReviewState review){
        this.reviews_states.add(review);
    }
    
}