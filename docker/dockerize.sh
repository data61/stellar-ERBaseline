#!/usr/bin/env bash

################################################################################
# Stellar docker image building and publishing script.
#
# Copyright 2017-2018 CSIRO Data61
#
script_version="0.2"
#
# Released under the Apache License, Version 2.0
# See http://www.apache.org/licenses/LICENSE-2.0
################################################################################

################################################################################
# Project specific configuration.

# Usage: in your Git repository, place this script and its configuration file at
# the same level as your Dockerfile.  See the included dockerize.md for detailed
# usage instructions.

# Get script directory, to reference sibling files (e.g. Dockerfile).
script_dir="$( cd "$(dirname "$0")" ; pwd -P )"

set -a
. "$script_dir/dockerize.config"
set +a

################################################################################
## Loggers ##
#
# we define standard loggers: info, warn, error, fatal and debug if `verbose`
# is defined. Loggers print to stderr with log level colourised according to
# severity.

enable_log_colours() {
    r=$(printf "\e[1;31m")      # red       (error, fatal)
    g=$(printf "\e[1;32m")      # green     (info)
    y=$(printf "\e[1;33m")      # yellow    (warning)
    b=$(printf "\e[1;34m")      # blue      (debug)
    m=$(printf "\e[1;35m")      # magenta   (process name)
    c=$(printf "\e[1;36m")      # cyan      (timestamp)
    x=$(printf "\e[0m")         # reset     (log message)
}

if [ -t 2 ]; then
    # only if standard error is connected to tty (not redirected)
    enable_log_colours
fi

# log formatter - do not use directly, use the predefined log levels below
prog_name="$(basename "$0")"
date_format="%F %T %Z"   # YYYY-MM-DD HH:MM:SS ZZZZ
logger() {
    local prefix="${m}${prog_name}: ${c}$(date "+${date_format}") $prefix"
    local i
    if [ "$#" -ne 0 ]; then
        for i; do           # read lines from args
            echo "${prefix}${i}${reset}" >&2
        done
    else
        while read i; do    # read lines from stdin
            echo "${prefix}${i}${reset}" >&2
        done
    fi
}

# log levels. Usage either:
#   <level> "line 1" "line 2" "line 3" ;
#   or to prefix each line of output from a child process
#   process | <level> ;
info()  { local prefix="${g} Info:${x} " ; logger "$@" ; }
warn()  { local prefix="${y} Warn:${x} " ; logger "$@" ; }
error() { local prefix="${r}Error:${x} " ; logger "$@" ; }
fatal() { local prefix="${r}Fatal:${x} " ; logger "$@" ; exit 1 ; }
debug() {
    [ -z "$verbose" ] || {
        local prefix="${b}Debug:${x} "
        logger "$@"
    }
}

################################################################################
# Utilities

# Get the repository slug (e.g. data61/stellar-py) from git configuration.
# Parms: none
# Return: repository slug
get_slug() {
    # Git repo origin, e.g. https://user@github.com/data61/stellar-ingest.git
    # Remove the trailing '.git'
    local orig=$(git config --get remote.origin.url|sed 's/\.git$//') res="$?"
    # Check for an error in executing git.
    if [ ! "$res" -eq "0" ]; then
        fatal "Could not read git repository origin."
        return $res
    # Running in a non-git directory just returns no origin.
    elif [ -z "$orig" ]; then
        fatal "This does not seem to be a git repository."
        return 1
    fi
    # info "Found git origin: $orig.git"
    # Parse the origin URL. Instead of just pulling out the slug, check that the
    # data make sense for a Stellar module, otherwise return error.
    local owner project
    base=$(echo $orig|cut -d"/" -f1-3) ||
        { fatal "Error parsing git repository origin."; return 1; }
    owner=$(echo $orig|cut -d"/" -f4) ||
        { fatal "Error parsing git repository origin."; return 1; }
    project=$(echo $orig|cut -d"/" -f5) ||
        { fatal "Error parsing git repository origin."; return 1; }
    if [[ ! $base =~ https://([-a-zA-Z0-9]+@)?github.com ]] ||
       [[ ! $owner = "data61" ]] ||
       [[ ! $project =~ ^stellar- ]]; then
        fatal "This does not seem to be an official Stellar repository."
        return 1
    fi

    # Turn project name into lower-case for docker image
    project=$(echo $project | awk '{print tolower($0)}')

    # Return the repo slug.
    echo "$(echo $owner/$project | awk '{print tolower($0)}')"
}

# Get the project identifier for the repository slug (e.g. stellar-ingest)
# Parms: none
# Return: project identifier
get_project_id() {
    # Try to get the project slug, calling get_slug().
    local slug project_id
    slug=$(get_slug) || return
    project_id=$(echo $slug|cut -d"/" -f2) ||
        { fatal "Error parsing git project id from slug."; return 1; }
    echo $project_id
}

# # Create a temporary directory using project id and PID
# # Parms: main script process id
# # Return: the temp directory path
# create_temp_dir() {
#     local pid=$1
#     if [[ ! $pid =~ [0-9]+ ]]; then
#         fatal "Invalid process id: \"$pid\""
#         return 1
#     fi
#     local proj_id temp_dir
#     proj_id=$(get_project_id) || return
#     temp_dir="/tmp/$proj_id-tmp-$script_pid"
#     mkdir -p $temp_dir 2> /dev/null ||
#         { fatal "Error creating temp directory $temp_dir."; return 1; }
#     echo $temp_dir
# }

# Get the version string for the current project.
# Parms: none
# Return: version string
get_version() {
    # Try to get the version string from the project definition.
    local version=$(eval "$version_cmd 2> /dev/null") res="$?"
    if [ ! "$res" -eq "0" ]; then
        fatal "Error executing: $version_cmd"
        fatal "Could not get project version."
        return $res
    fi
    # Just in case, let's check the version string makes sense.
    if [[ ! $version =~ $snapshot_re ]] && [[ ! $version =~ $release_re ]]; then
        fatal "Invalid version string: \"$version\"."
        return 1
    fi
    # All good, log the version and return it.
    echo $version;
}

# Based on project version, pick a generic docker tag (latest, snapshot, etc.)
# Parms: none
# Return: docker tag string
get_docker_tag() {
    # Try to get the project version, calling get_version().
    local version tag
    version=$(get_version) || return
    if [[ $version =~ $snapshot_re ]]; then
        tag="snapshot"
    elif [[ $version =~ $release_re ]]; then
        tag="latest"
    else
        # Note: this should never happen.
        fatal "Invalid version string: \"$version\"."
        return 1
    fi
    echo $tag
}

# Copy  files needed  for the  container to  the temporary  directory. The list must
# include the Dockerfile (issue a warning if no files is called Dockerfile).
# Parms: none. Use global variables scriptdir, tmpdir and files.
# Return: the (possibly expanded) basename of the Dockerfile.
copy_files() {
    # Append the Dockerfile to the list of files to copy.
    files+=("$script_dir/$dockerfile")

    # Copy one by one the files specified in the array 'files'.
    local ff
    for f in "${files[@]}"; do
        # Deferred expansion of "~/" in double quotes and escaped vars: e.g. \$version
        ff=$(eval "echo $f")

        # If it's a relative path, it must be relative to to this script's directory: prepend it.
        if [[ ! "$ff" =~ ^/ ]]; then
            ff="$script_dir/$ff"
        fi

        # Check that the file is accessible and copy it to temp directory.
        if [ -r "$ff" ]; then
            info "Found file: $ff"
            cp -r $ff $tmpdir ||
                { fatal "Unexpected error on file: $ff"; return 1; };
        else
            fatal "Cannot read file: $ff"
            return 1
        fi
    done
    echo $tmpdir/$(basename $ff)
}

################################################################################
# MAIN

# Identify the project.
info "Identifying current project."
proj_slug=$(get_slug) || { fatal "Exiting script."; exit 1; }
info "Found valid project slug: $proj_slug"
proj_id=$(get_project_id) || { fatal "Exiting script."; exit 1; }
info "Found valid project ID: $proj_id"

# Create a temporary working directory, that will be deleted on exit.
info "Creating docker build temporary directory."
tmpdir=$(mktemp -d -t "$proj_id-tmp.XXXXXXXXXX") ||
    { fatal "Error creating temp dir. Exiting script."; exit 1; }
info "Created temp dir: $tmpdir"
cleanup() { rm -rf "$tmpdir"; }
trap cleanup EXIT

info "Getting version string from project definition."
version=$(get_version) || { fatal "Exiting script."; exit 1; }
info "Found valid version string: \"$version\"."

info "Generating docker tags from version string."
tag=$(get_docker_tag) || { fatal "Exiting script."; exit 1; }
info "Docker image will be tagged as: \"$version\" and \"$tag\"."

# Copy necessary files to the temp directory and use as build context.
# TODO: make this an associative array, host-path/docker-path?
info "Processing files required for Docker container."
dfile=$(copy_files) || { fatal "Exiting script."; exit 1; }
info "Ready to build image with dockerfile: $dfile"

# Build the local Docker image, unless explicitly blocked.
if [ -z "$STELLAR_NO_BUILD" ]; then
    info "Building image with dockerfile: $dfile"
    info "Build image: $proj_slug:$version"
    docker build -f $dfile -t $proj_slug:$version $tmpdir ||
        { fatal "Docker error. Exiting script."; exit 1; }
    info "Build image: $proj_slug:$tag"
    docker tag $proj_slug:$version $proj_slug:$tag ||
        { fatal "Docker error. Exiting script."; exit 1; }
else
    warn "Environment var STELLAR_NO_BUILD is set."
    error "Exiting without building."
    exit 1;
fi

# Publish the Docker image. Only do it from travis or if forced.
if [ ! "$CI" = "true" ] || [ ! "$TRAVIS" = "true" ]; then
    if [ -z "$STELLAR_FORCE_PUBLISH" ]; then
        warn "Script appears to be running outside Travis. Set STELLAR_FORCE_PUBLISH at your own risk!"
        error "Exiting script."
        exit 1
    else
        warn "Script appears to be running outside Travis, but STELLAR_FORCE_PUBLISH set."
        # Outside of Travis, the script is assument to be interactive. Check if user is logged in.
        duser=$(docker info 2> /dev/null |grep Username)
        if [ -z "$duser" ]; then
            docker login ||
                { fatal "Docker error. Exiting script."; exit 1; }
        fi
    fi
else
    # Inside Travis login uses encrypted variables.
    # docker login -e $DOCKER_EMAIL -u $DOCKER_USER -p $DOCKER_PASS
    docker login -u "$DOCKER_USER" -p "$DOCKER_PASS" ||
        { fatal "Docker error. Exiting script."; exit 1; }
fi

# If the script arrived here, publish the image (unless explicitly denied).
if [ -z "$STELLAR_NO_PUBLISH" ]; then
    info "Publishing image: $proj_slug:$version"
    docker push $proj_slug:$version ||
        { fatal "Docker error. Exiting script."; exit 1; }
    info "Publishing image: $proj_slug:$tag"
    docker push $proj_slug:$tag ||
        { fatal "Docker error. Exiting script."; exit 1; }
else
    warn "Environment var STELLAR_NO_PUBLISH is set."
    error "Exiting without publishing."
    exit 1;
fi

################################################################################
# CHANGELOG

# v.0.2 - 23/2/2018 - Generalization for all stellar projects
#   - Script was renamed to dockerize.sh.
#   - Script now loads  a project-specific configuration file,  with commands to
#     obtain package version, list of files needed for the docker container.
#   - Following project  specific configuration,  checks are made  (e.g.  docker
#     image  tags are  derived from  source  version, images  are not  published
#     unless running on CI, etc.) and the docker image is created/published.

# v.0.1 - 1/1/2018 - Initial docker creation script for stellar-ingest



