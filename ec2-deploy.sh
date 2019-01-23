#!/bin/bash
KEY="~/.ssh/harri-aws.pem"
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
HOSTNAME=''

function init {
	echo "Initialising EC2 instance..."
	ssh -i $KEY ec2-user@$HOSTNAME 'wget -O - https://gist.githubusercontent.com/HarriBellThomas/18765ab05474a1f38d3a870147f90374/raw/18457d029ca38ead14c7c7c8ccad3f8a68497003/bubblegum-setup.sh | bash' > /dev/null 2>&1
	update
	echo "VM prepared."
}

function update {
	echo "Copying files..."
	scp -i $KEY $DIR/target/bubblegum-core-1.0.0-jar-with-dependencies.jar $DIR/simulation.yml $DIR/log4j.xml ec2-user@$HOSTNAME:~ > /dev/null 2>&1
}

if [ $# -gt 1 ]; then
	HOSTNAME=$1
	if [ $2 == "sim" ]; then
		ssh -i $KEY ec2-user@$HOSTNAME 'wget -O - https://gist.githubusercontent.com/HarriBellThomas/2de5b5d214d05b055044e77dc1d68432/raw/7a6c99404bb2ac8110022413e9000ee0c26678c7/bubblegum-simulate.sh | bash' > /dev/null 2>&1
	elif [ $2 == "stat" ]; then
		ssh -i $KEY ec2-user@$HOSTNAME 'tail -f output.txt; bash -l'
	elif [ $2 == "c" ]; then
		init
	elif [ $2 == "u" ]; then
		update
	elif [ $2 == "i" ]; then
		ssh -i $KEY ec2-user@$HOSTNAME
	elif [ $2 == "r" ]; then
	 	scp -pr -i $KEY ec2-user@$HOSTNAME:~/logs/ $DIR/recordings/$3
	elif [ $2 == "chain" ]; then
		init
		ssh -i $KEY ec2-user@$HOSTNAME 'export BOOTSTRAP "$3"'
		sim
		# stat
	fi
fi
