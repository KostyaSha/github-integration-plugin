package org.jenkinsci.plugins.github.pullrequest.utils;

import org.jenkinsci.plugins.github.pullrequest.utils.ReviewState;
import java.util.List;
import java.util.ArrayList;

/**
 * Save the state of the given Pull request event in order to keep track of the approval states of the reviews.
 * @author Nicola Covallero
 */
public class PRApprovalState {
    private List<ReviewState> reviews_states;
    
    public PRApprovalState() {
        this.reviews_states = new ArrayList<ReviewState>();
    }

    public void setReviews_states (List<ReviewState> reviews_states){
        this.reviews_states = reviews_states;
    } 

    public List<ReviewState> getReviews_states(){
        return this.reviews_states;
    }

    public void addReviewState(ReviewState review){
        this.reviews_states.add(review);
    }
    
}