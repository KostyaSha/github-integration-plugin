//package org.jenkinsci.plugins.github.pullrequest;
//
//import hudson.Extension;
//import hudson.model.AbstractProject;
//import hudson.model.UnprotectedRootAction;
//import hudson.security.ACL;
//import jenkins.model.Jenkins;
//import org.acegisecurity.Authentication;
//import org.acegisecurity.context.SecurityContextHolder;
//import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
//import org.kohsuke.github.GHEventPayload;
//import org.kohsuke.github.GHRepository;
//import org.kohsuke.github.GitHub;
//import org.kohsuke.stapler.StaplerRequest;
//import org.kohsuke.stapler.StaplerResponse;
//
//import java.io.IOException;
//import java.io.StringReader;
//import java.util.ArrayList;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//
///**
// * Hook handler for GitHubPR.
// *
// * @author Kanstantsin Shautsou
// * @author Alina Karpovich
// */
//public class GitHubPRRootAction {
//    //TODO not tested, so removed
//}