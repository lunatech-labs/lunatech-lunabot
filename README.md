# lunabot

## Build & Run

```sh
$ cd lunabot
$ ./sbt
> container:start
> browse
```


If `browse` doesn't launch your browser, manually open [http://localhost:8080/](http://localhost:8080/) in your browser.


## Standalone Deployment


```sh
sbt clean assembly
java -jar target/scala-2.11/lunabot-assembly-0.1.0-SNAPSHOT.jar
```

You can copy the generated assembly jar and run it on the server.

## Security

CAVEAT: Current implementation spawns scala REPL for each /scala command/code without security check. 
Future version should use a locked-down REPL or code evaluator with risky command/code/API libs disabled and 
constrained in a sandbox such as chroot jail. 

## Limitations

Since scala REPL in the interactive mode requires direct interaction with the console/tty, it can't work with nohup. 
The Lunabot servlet needs to be run in screen or tmux to keep the process active after terminal session logout.
A wrapper over [com.twitter.util.Eval](http://twitter.github.io/util/docs/#com.twitter.util.Eval) or 
[Remote Ammonite-REPL](https://lihaoyi.github.io/Ammonite/#RemoteREPL) might be a better solution.
