# Connect X
A program made to play a generalized version of connect 4 perfectly 

# Commands
## Compile 
In the project directory run:
```
$ make
```

## CXGame application:
Human vs Computer.  In the build/ directory run:
```
$ java connectx.CXGame 6 7 4 connectx.L0.L0
```

Computer vs Computer. In the build/ directory run:
```
$ java connectx.CXGame 6 7 4 connectx.L0.L0 connectx.L1.L1
```

## CXPlayerTester application:
All commands must be run in the build/ directory. Output score only:
```
$ java connectx.CXPlayerTester 6 7 4 connectx.L0.L0 connectx.L1.L1
```

Verbose output
```
$ java connectx.CXPlayerTester 6 7 4 connectx.L0.L0 connectx.L1.L1 -v
```

Verbose output and customized timeout (1 sec) and number of game repetitions (10 rounds)
```
$ java connectx.CXPlayerTester 6 7 4 connectx.L0.L0 connectx.L1.L1 -v -t 1 -r 10
``` 
