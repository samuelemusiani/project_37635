# Connect X

A program made to play a generalized version of connect 4 perfectly 

# Commands

## Compile 

If you are on Linux or macOS you can simply run the following command in the 
project directory:
```sh
$ ./gradlew build
```
If you are so unlucky to work on Windows, the following command should work:
```
$ gradlew build
```

## CXPlayerTester application:

After the project built successfully, you can test computer vs computer with 
the following command:
```sh
$ java -jar build/libs/connectx.jar 6 7 4 connectx.L0.L0 connectx.L1.L1  
```
If you are on Windows remember to change the `/` to the `\` for the path.

There are additional flags to use in order to specify different configurations:

- With `-v` the output will be verbose
- With `-t` you can specify a maximum time of each move
- With `-r` you can specify how many rounds need to be played.

Verbose output and customized timeout (1 sec) and number of game repetitions 
(10 rounds) should look like this:
```sh
$ java -jar build/libs/connectx.jar 6 7 4 connectx.L0.L0 connectx.L1.L1 -v -t 1 -r 10
``` 

## CXGame application:

In `gradle.build` the main class is specified, if you want another class to run
as main (for example the CXGame) you can simply uncomment the commented line.

## full_test script
There is a bash script with the name `full_test.sh` that allow to use the 
`CXPlayerTester` with all the board configurations stored in `board_configurations.txt`.
The use is pretty simple: you just need to pass two player names in the command line
and the script will do the rest.
```sh
./full_test.sh L0 L1
```
If you want the verbose output just use the '-v' flag.
