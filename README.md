GitHub Pull Request Plugin
==========================
This is rewritten version of [ghprb-plugin](https://wiki.jenkins-ci.org/display/JENKINS/GitHub+pull+request+builder+plugin) that after many years is in complex code state when keeping backward compatibility with fixing all bugs almost impossible.
Code inspired by github-plugin, git-plugin, ghprb-plugin and others.

Description:

- By design it extensible and new features can be added easily
- Restriction features are not bundled into main code and can be extended separately. Available as API for doing checks from other plugin parts
- Trigger events are splitted to separate classes and can do:
  - Label: added, removed, exists
  - PR states: closed, opened, commit changed
  - Comment triggering
- UI/configuration changes:
  - No tones of checkboxes
  - Trigger section contains only configuration related to triggering
  - All messages configurable
- Publishers are in post-build actions:
  - slitted to separate publishers to have ability choose required order
    - Set build status. Job can configured to not set PR statuses
    - Add labels
    - Remove labels
    - Close PR
    - Post comment with TokenMacro and Groovy Templates from [email-ext-plugin](https://wiki.jenkins-ci.org/display/JENKINS/Email-ext+plugin) support
- Trigger check modes (planned):
  - Lightweight using GH hooks without persisting state (penalty: lost events)
  - Hooks with persisting state
  - Cron checks with persistence
- Built sources variants:
  - Build only merged state. Note: merged to target branch only in moment when PR was built.
  - Build only 'head' state. This is what is really contains in PR without merge to target branch.
  - Conditional build with `GITHUB_PR_COND_REF`. When PR is mergeable it contains 'merge' word and 'head' when not.
- Persisted state is stored near build configuration in `*.runtime.xml` file to exclude useless saves with [jobConfigHistory-plugin](https://wiki.jenkins-ci.org/display/JENKINS/JobConfigHistory+Plugin)
- All input variables starts from `GITHUB_PR_*`
- Refspec for [git-plugin](https://wiki.jenkins-ci.org/display/JENKINS/Git+Plugin) is not hardcoded, branch specifier can be constructed using input variables
- Configuration and code tried to be workflow friendly

Integration with [github-plugin](https://wiki.jenkins-ci.org/display/JENKINS/GitHub+Plugin) can

- optimize/reuse code for:
  - searching projects
  - using one hook url
  - hooks configuration (with periodic cleaning?)
  - have one Global configuration
  - reuse credentials lookup

TODO:

 - [ ] review injected variables in Cause and String parameters
 - [ ] integrate with git-plugin by providing patch for BuildData get through BuildChooser?
 - [ ] implement hooks triggering
 - [ ] more tests

Configuration:
 - Set GitHub project property with link to your GH repository
 - Add any repo name i.e. 'origin-pull' and set refspec to `+refs/pull/${GITHUB_PR_NUMBER}/merge:refs/remotes/origin-pull/pull/${GITHUB_PR_NUMBER}/merge` if you want run build for merged state or '/head' for building exact PR commits, or `$GITHUB_PR_COND_REF` if you want 'head' state when PR is not mergeable (according to GH state). Set branch specifier to `origin-pull/pull/${GITHUB_PR_NUMBER}/merge`. This exact link allows to speedup fetch sources.
 - If you want do gatekeepering, then add second repository with i.e. origin. Add "Merge" extension in from Git SCM and configure post build action for push action.
