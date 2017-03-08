# Changelog

## [1.1.0-SNAPSHOT] - 2017-03-08

### Changes

  - testware is updated to use a new version of `bz-checker` library properly
  
     - replaced `catch XmlRpcException` by `catch BugzillaAPIException`
     - removed `catch Exception` from the whole testware
