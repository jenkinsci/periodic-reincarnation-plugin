Restart failed jobs periodically. The restart happens according to a given cron time and the failed jobs being restarted can be filtered with the help of regular expressions.
There is also the additional option to restart jobs if the last build has failed but the second last was a success and there were no changes between these two builds.

This can be useful when something temporary may be causing a build to fail.
For instance:

	- there was not enough space on the hard disk
	
	- there was some incompatibility of some kind

Another example:
It is easy to periodically restart all jobs that have failed because of some arbitrary "error_failing_the_build". Just activate the plugin, set the cron time ("* * * * *" for every minute), and add this "error_failing_the_build" as regular expression.

The configuration for this plugin can be found in the global configuration page of Jenkins under "Periodic Reincarnation of failed builds". 