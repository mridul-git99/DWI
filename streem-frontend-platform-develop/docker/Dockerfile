# Stage 0, "builder", based on Node.js, to build and compile the frontend
ARG NODE_VERSION

# use the base image
FROM node:${NODE_VERSION}-alpine as builder

ARG VERSION

ARG BACKEND_URL

ARG COMMIT

MAINTAINER infra@leucintech.com

RUN echo Using image: node:${NODE_VERSION}-alpine

# change the current working directory
WORKDIR /app

# copy package.json and package-lock.json
COPY package*.json /app/

# https://docs.npmjs.com/cli/v6/commands/npm-ci
RUN npm ci

# copy the current directories & files
COPY ./ /app/

# build the project
RUN npm run build

# create a version.txt file
RUN echo "Leucine: $VERSION" > /app/dist/version.txt

# create a internal-version.txt file
RUN echo "Leucine (Internal): $COMMIT" > /app/dist/internal-version.txt

# Stage 1, based on Nginx, to have only the compiled app, ready for production with Nginx
FROM nginx:stable-alpine

# set environmental variable
ENV BACKEND_URL=$BACKEND_URL

# copy the dist folder to the nginx html
COPY --from=builder /app/dist/ /usr/share/nginx/html

# copy the nginx.conf
COPY --from=builder /app/docker/nginx/nginx.conf /etc/nginx/nginx.conf

# copy the entrypoint.sh
COPY --from=builder /app/docker/nginx/entrypoint.sh /etc/nginx/entrypoint.sh

# expose port 80
EXPOSE 80

# give executable permission to entrypoint.sh
RUN ["chmod", "+x", "/etc/nginx/entrypoint.sh"]

# give entrypoint
ENTRYPOINT [ "ash", "/etc/nginx/entrypoint.sh" ]
