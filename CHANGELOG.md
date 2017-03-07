# Changelog

## - 2017-03-07

### Changes

  - testware is updated to use new version of `bz-checker` library properly
  
     - replaced `catch XmlRpexception` by `catch BugzillaAPIException`
     - removed `catch Exception` from the whole testware
