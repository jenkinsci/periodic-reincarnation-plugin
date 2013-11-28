Restart and execute an action over builds with infrastructure failures.


This plugin will let you define a set of regular expressions each one with a Groovy script. Failed jobs are checked periodically. If any of the regular expressions defined in the plugin match against the build output, the defined Groovy scripts will be executed and the build will be restarted. The detection happens according to a given cron time also defined in the plugin.

There is also the additional option to restart jobs if the last build has failed but the second last was a success and there were no changes between these two builds.

This plugin can be useful when something temporary may be causing a build to fail.
For instance:

- there was not enough space on the hard disk. Upon that event a disk clean can be automatically performed.
	
- there was a communication problem with a slave. A script to disconnect such agent is provided.

- there was some incompatibility of some kind

Another example:
It is easy to periodically restart all jobs that have failed because of some arbitrary "error_failing_the_build". Just activate the plugin, set the cron time ("* * * * *" for every minute), and add this "error_failing_the_build" as regular expression.

The configuration for this plugin can be found in the global configuration page of Jenkins under "Periodic Reincarnation of failed builds".

Example of Groovy scripts can be found here: https://github.com/jenkinsci/periodic-reincarnation-plugin/tree/master/scripts
