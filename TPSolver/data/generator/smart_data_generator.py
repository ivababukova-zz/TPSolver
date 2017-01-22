from pprint import pprint
import json
import random
import sys
from decimal import Decimal

def airport_names():
    with open("airports.json") as data_file:
        data = json.load(data_file)
        airs_json = data["airports"]
    airs = []
    for a in airs_json:
        airs.append(a.get('airport'))
    return airs


def get_n_airport_names():
    airs = []
    for i in range(0, n):
        rand_num = random.randint(0, len(pool) - 1)
        airs.append(pool[rand_num])
        pool.pop(rand_num)
    return airs
    
def get_n_airports():
    airports = []
    dest_num = 0
    home_point = random.randint(0, len(airport_names)-1)

    for i in range(0, len(airport_names)):
        purpose = ""
        rand_conn = round(random.uniform(min_conn, max_conn ), 1)
        if i == home_point:
            purpose = "home_point"
        elif dest_num == d:
            purpose = "connection"
        else:
            purpose = "destination"
            dest_num += 1

        a = {
            "name": airport_names[i],
            "connection_time": rand_conn,
            "purpose": purpose
        }
        airports.append(a)
    return airports
    
def tsp_seed():
    dests = []
    hp = ""
    aips = allairports
    for airport in aips:
        if airport["purpose"] == "destination":
            dests.append(airport["name"])
        if airport["purpose"] == "home_point":
            hp = airport["name"]
    route = []
    route.append(hp)
    while len(dests) > 0:
        seed = random.randint(0, len(dests) - 1)
        route.append(dests[seed])
        dests.pop(seed)
    route.append(hp)
    return tsp(route, hp)
    
    
def sum_conn_time(route_seed, hp):
    suma = 0
    for a in route_seed:
        if a != hp:
            a_conn = [d["connection_time"] for d in allairports if d["name"] == a]
            suma = suma + round(a_conn[0], 2)
    return suma
    
    
def generate_flight(id, dep, arr, date, duration, cost):
    return {
            "id": id,
            "dep_airport": dep,
            "arr_airport": arr,
            "date": date,
            "duration": duration,
            "price": cost
        }

def divide_flight(f, id1, id2):
    airps = allairports
    dep = f["dep_airport"]
    arr = f["arr_airport"]
    pair = []
    arr1 = airps[random.randint(0, len(airps)-1)]["name"]
    while arr1 == dep or arr1 == arr:
        arr1 = airps[random.randint(0, len(airps)-1)]["name"]
    
    conn_time = [d["connection_time"] for d in allairports if d["name"] == arr1]
    conn_time = round(conn_time[0], 2)
    D = (round(f["duration"], 2) - conn_time)/2
    print("D 1: ", D)
    if min_dur > D:
        print("this flight is undivisible 1. ", D)
        return(-1)
    edate1 = round(f["date"], 2)
    duration1 = round(random.uniform(min_dur, D), 2)
    date1 = round(random.uniform(edate1, edate1 + D - duration1), 2)
    cost1 = round(random.uniform(min_cost, max_cost), 2)
    
    edate2 = date1 + duration1 + conn_time
    print("new conn time: ", conn_time)
    print("earliest date 1: ", edate1, " earliest date 2: ", edate2)
    D = round(f["date"], 2) + round(f["duration"], 2) - (date1 + duration1 + conn_time)
    if min_dur > D:
        print("this flight is undivisible 2. ", D)
        return(-1)
    print("D 2: ", D)
    duration2 = round(random.uniform(min_dur, D), 2)
    date2 = round(random.uniform(edate2, edate2 + D - duration2), 2)
    cost2 = round(random.uniform(min_cost, max_cost), 2)
    f1 = generate_flight(id1, dep, arr1, date1, duration1, cost1)
    f2 = generate_flight(id2, arr1, arr, date2, duration2, cost2)
    print(f1)
    print(f2)
    print("-------------------------------------------------------")
    pair.append(f1)
    pair.append(f2)
    return(pair)
    
def divide_flights(flights):
    next_id = len(flights) + 1
    while(m > len(flights)):
        for f in flights:
            print(f)
            pair = divide_flight(f, next_id, (next_id + 1))
            if pair != -1:
                print("division successful")
                flights.append(pair[0])
                flights.append(pair[1])
                next_id = len(flights) + 1
            pair = []
    print("**********that is all:********")
    pprint(flights)
    print("******************************")
    return flights
    
def tsp(route_seed, hp):
    print(route_seed)
    route_flights = []
    prev_duration = 0
    C = sum_conn_time(route_seed, hp)
    duration_sum_temp = 0
    D = (T - C)/d # the average duration + stay on destination time allowed for each flight, assuming that traveller always travels
    edate = 0
    if D < 0:
        print("The current instance won't have solutions. Please re-run the program")
        exit(1)
    
    for i in range(0, len(route_seed)-1):
        dep = route_seed[i]
        arr = route_seed[i+1]
        conn_time = [d["connection_time"] for d in allairports if d["name"] == arr]
        conn_time = round(conn_time[0], 2)
        if i != 0 and i != len(route_seed)-1:
            edate += conn_time
        ldate = edate + D
        md = max_dur
        if md > D:
            md = D
        duration = round(random.uniform(min_dur, md), 2)
        date = round(random.uniform(edate, ldate - duration), 2)
        cost = round(random.uniform(min_cost, max_cost), 2)
        prev_arr_date = date + duration
        
        f = generate_flight(i+1, dep, arr, date, duration, cost)
        duration_sum_temp += duration
        route_flights.append(f)
        C = C - conn_time
        D = (T - (duration + C + date))/(d - i + 1)
        edate = date + duration
    return divide_flights(route_flights)
        
    
def get_n_flights(seed_flights_len):
    print("******Now random flights:******")
    airps = allairports
    flights = []
    
    for i in range(0, m - seed_flights_len -1):
        dep_num = random.randint(0, len(airps)-1)
        allowed_arr = list(range(0, dep_num)) + list(range(dep_num+1, len(airps)-1))
        arr_num = random.choice(allowed_arr)
        max_date = T - max_dur # if max date is left to be T, many flights depart too late to be useful
        dep = airps[dep_num]["name"]
        arr = airps[arr_num]["name"]
        date = round(random.uniform(0, max_date), 2)
        duration = round(random.uniform(min_dur, max_dur), 2)
        cost = round(random.uniform(min_cost, max_cost), 2)
        f = generate_flight(i + 1 + seed_flights_len, dep, arr, date, duration, cost)
        print(f)
        flights.append(f)
        
    return flights

def construct_json():
    data_name = "../random_data/" + str(m) + "_" + str(n) + "_" + str(d) + "_" + id + ".json"
    with open(data_name, "w") as outfile:
        json.dump(data, outfile, sort_keys=True, indent=4)
    print("Successfully written data to file ", data_name)


# run: python data_generator.py n m T max_cost
if len(sys.argv) != 8:
    print("usage: python data_generator.py "
          "\n   <number of flights>"
          "\n   <number of airports>"
          "\n   <number of destinations>"
          "\n   <holiday time>"
          "\n   <min_flight_cost>"
          "\n   <max_flight_cost>"
          # "\n   <min_duration>"
          # "\n   <max_duration>"
          # "\n   <min_connection_time>"
          # "\n   <max_connection_time>"
          "\n   <number to be appended to the name of the output file>"
          )
    exit(1)

# number of flights:
m = int(sys.argv[1])
# number of airports:
n = int(sys.argv[2])
# max number of destinations:
d = int(sys.argv[3])
# holiday time:
T = int(sys.argv[4])
# minimum flight cost:
min_cost = int(sys.argv[5])
# maximum cost:
max_cost = int(sys.argv[6])
# minimum flight duration:
min_dur = 0.1
# maximum flight duration:
max_dur = 1.5 # float(sys.argv[8])
# minimum connection time
min_conn = 0 # float(sys.argv[9])
# maximum connection time
max_conn = 1 # float(sys.argv[10])
# output file id
id = sys.argv[7]

if m <= d or m < (d + 1):
    print("The number of flights must be significantly bigger than the number of airports.")
    exit(1)

if n < d:
    print("Can't have more destinations than airports.")
    exit(1)

pool = airport_names()
if n > len(pool):
    print("The number of airports should be less or equal to ", len(pool))
    exit(1)

# returns the names of the used airports
airport_names = get_n_airport_names()

# returns a list of dict with constructed airports
allairports = get_n_airports()

seed_trip = tsp_seed()

# returns a list of dict with constructed flights
allflights = get_n_flights(len(seed_trip))
for flight in seed_trip:
    allflights.append(flight)

# pprint(allflights)

data = {
    "airports": allairports,
    "flights": allflights,
    "holiday_time": T
    }

# write the data to a json file
construct_json()
