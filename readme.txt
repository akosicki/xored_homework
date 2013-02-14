Task description:
The test is to create a small eclipse plugin, which contribute new Launch
Configuration Type, say "Composite Launch". So plugin users will be able to
create "composite" launch configurations.
Also users shall be able to add existing configurations (of any type) into
composite one. When user execute "composite" launch -> this result in all the
contained configurations will be launched.

Release notes:
* Label and content providers from the original debug plugin are reused so that
user should see list with the launch configurations similar to the one on the
left hand side. For the same purpose even incorrect entries are displayed.
* I am aware some non-API types are used
* I've added guava and jsr305 annotations as the only dependencies (I am really
used to them)
* Tested only on GTK/Linux, on the classic eclipse release 4.2.1
* No NLS support
* No automatic tests

Possible improvements:
* Proper handling of the 'orphaned' configurations. E.g. user selects some conf 
as a child of some other composite conf and later on deletes the child conf.
* Opening different consoles for different launch configuration executions
* closer look at the launch_group-related stuff (there might be some bugs)