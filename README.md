# Distributed Flight Reservation System
This is a simple implementation of Paxos algorithm.

In this project, you are allowed to manage flight reservations among multiple sites. The system would also provide fault tolerance, especially for message loss and site crashes. The project did not implement full Paxos (i.e. it did not apply any leader election algorithm for distinguished proposer).



**This repository contains:**

- `lib`: the libraries used in the project
- `src`: contains source code of the project
- `knownhosts.json`: a list of hosts that provide the flight reservation service
- `build.sh`: a script to compile source code
- `run.sh`: a script to run the application code



**Programming Language**

- Java (1.8 or higher version)



See detail implementation instruction [here](Report.pdf).



## I. Install

#### Deployment

Before setup, you need to make sure all the sites are in the same network so that they can communicate with each other. Then modify the `knownhosts.json` file, which specifies the host information.

This is an example of `knownhosts.json` file that contains 3 sites.

```json
{
  "hosts": {
    "apple": {
      "tcp_start_port": 9006,
      "tcp_end_port": 9008,
      "udp_start_port": 15006,
      "udp_end_port": 15008,
      "ip_address": "172.17.0.2"
    },
    "banana": {
      "tcp_start_port": 9009,
      "tcp_end_port": 9011,
      "udp_start_port": 15009,
      "udp_end_port": 15011,
      "ip_address": "172.17.0.3"
    },
    "cherry": {
      "tcp_start_port": 9012,
      "tcp_end_port": 9014,
      "udp_start_port": 15012,
      "udp_end_port": 15014,
      "ip_address": "172.17.0.4"
    }
  }
}
```

#### Compile

Execute `build.sh` to compile source code. 

```shell
$ ./build.sh
```

This would generate a `bin/` directory that contains all the class files and relevant files.

#### Run

Copy the `bin/` directory to each sites specified in `knownhosts.json`. Then run the application using host name as input.

```shell
$ ./run.sh [hostname]
```



## II. Usage

The flight reservation service would allow users to reserve or cancel reservations, as well as display the list of reservations or the contents of log.

1. To make a reservation, the user enter the following

   ```
   % reserve <client_name> <CSV_list_of_flight_numbers>
   ```

2. A client can cancel their flight reservation by entering

   ```
   % cancel <client_name> 
   ```

3. To display the list of all reservations, enter the following command

   ```
   % view
   ```

4. To display the contents of log, enter the following command

   ```
   % log
   ```



## III. Authors

This project exists thanks to all the people who contribute.

- **Liangbin Zhu**
- **Brendan Cross**
