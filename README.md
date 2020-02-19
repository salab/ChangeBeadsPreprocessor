# ChangeBeadsPreprocessor
A preprocessor for [ChangeBeadsThreader](https://github.com/salab/ChangeBeadsThreader).

## Description
`ChangeBeadsPreprocessor` is a preprocessor which analyze a repository consisting of fine-grained commits and generate an input of ChangeBeadsThreader.

## Requirement
- Java 11+

## Installation
1. `$ git clone https://github.com/salab/ChangeBeadsPreprocessor.git`
1. `$ gradle build`

## Usage
- `java -jar fine-grained-commit-analyzer -s <repository path>`
- ChangeBeadsPreprocessor output the analysis result json file to the same directory as the input repository.

## Option
- -s, --src <path>
    - The input repository path
- -l, --log
    - Output log to standard output.
