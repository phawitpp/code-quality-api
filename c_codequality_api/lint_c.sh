#!/bin/bash
clang-tidy $1 -- -std=c11 > lint_output.txt
cat lint_output.txt