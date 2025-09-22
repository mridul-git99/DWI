# Introduction

TODO: Give a short introduction of your project. Let this section explain the objectives or the motivation behind this project.

# Getting Started

TODO: Guide users through getting your code up and running on their own system. In this section you can talk about:

1. Installation process
2. Software dependencies
3. Latest releases
4. API references

# Build and Test

TODO: Describe and show how to build your code and run the tests.

# Building Docker image

### Building with defaults

`./docker/script.sh`

### Building with custom backend end URL

`./docker/script.sh -a 'http://localhost:8080/v1'`

# Running Docker image

`docker run -p 80:80 -e BACKEND_URL='http://localhost:8080/v1' --name cleen-dwi-frontend leucine.azurecr.io/cleen-dwi/frontend:latest`

## TODO

- Use Local Fonts Rather Than Google Fonts

## Fonts To be used (.ttf as Pdf only Supports Those)

- https://fonts.google.com/specimen/Inter

## Notes:

- mui default z-index = 1300
- For react pdf always use ternary when checking for length of an array because react still renders 0, and react-pdf only allow rendering text in Text component, not in View and thats what breaks it.

## Documentation Reference:

**Please refer to the documentation of the version specified in package.json**

- MUIv4 - https://v4.mui.com/
- react-hook-form - https://react-hook-form.com/
- react-redux - https://react-redux.js.org/
- redux-saga - https://redux-saga.js.org/
- react-pdf - https://react-pdf.org/
