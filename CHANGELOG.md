## Changelog

## next

## 0.0.1-rc4
 * [PR] New label not exists event.

## 0.0.1-rc2, 0.1.0-rc3
 - Failed maven-release-plugin.
 
## 0.0.1-rc1
 * [PR] Fixed UI configs.

## 0.0.1-beta17

* [PR] Clarified help texts and UI info.
* [PR] Support Job.class (workflow-plugin) for hooks triggering. 
* [PR] Update block-queued-job-plugin and Extension for it to support Job.class (workflow-plugin)
* [PR] Added checkstyle, pmd.

## 0.0.1-beta16

* Fixed UI settings configuration.

## 0.0.1-beta15

* Support Job type (workflow-plugin support)
* Reorganised to multi-module maven structure
* Renamed to `github-integration-plugin`

## 0.0.1-beta14

* Provide default configuration in UI

## 0.0.1-beta13

* Indent trigger configuration UI
* Prevent NPE in weird missconfigured case
* Fixed check api url method in global config
* Replace custom repo name parsing with github-plugin class
* Fix NPE after pulls clearing in GHRepo action

## 0.0.1-beta12

* Optimise hooks based check
* Replace all jdk logging with slf4j-jdk

## 0.0.1-beta11

* Fixed trigger by comment UI entry

## 0.0.1-beta10

* Fix broken polling page
* Feature: Experimental hooks support
* Fix: Don't persist project object

## 0.0.1-beta9

* skip events refactoring
* clean-ups

## 0.0.1-beta8
Version forward: maven-release-plugin failed

## 0.0.1-beta7

* Polling log in project/build
* Waterfall conditioning
* Added blockedQueue optional Extension (block-queued-job-plugin)
* Fixed: Allow save job that had no GH project property before

