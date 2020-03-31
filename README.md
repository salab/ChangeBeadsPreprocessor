# ChangeBeadsPreprocessor
A preprocessor for [ChangeBeadsThreader](https://github.com/salab/ChangeBeadsThreader).

## Description
`ChangeBeadsPreprocessor` is a preprocessor which analyze a repository consisting of fine-grained commits and generate an input of ChangeBeadsThreader.

## Requirement
- Java 1.8+
- Kotlin 1.3+

## Installation
1. `$ git clone https://github.com/salab/ChangeBeadsPreprocessor.git`
1. `$ cd changebeadspreprocessor`
1. `$ ./gradlew build`
    - Output .jar file to `build/libs/`

## Usage
- `java -jar ChangeBeadsPreprocessor-1.0.0.jar -s <repository path>`
- ChangeBeadsPreprocessor output the analysis result json file to the same directory as the input repository.

## Option
- -s, --src <path>
    - The input repository path
- -l, --log
    - Output log to standard output.
