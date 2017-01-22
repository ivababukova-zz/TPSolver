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
        
        if i != 0 and i != len(route_seed)-1:
            conn_time = [d["connection_time"] for d in allairports if d["name"] == arr]
            edate += round(conn_time[0], 2)
            
        ldate = edate + D
        md = max_dur
        if md > D:
            md = D
        duration = round(random.uniform(min_dur, md), 2)
        date = round(random.uniform(edate, ldate - duration), 2)
        cost = round(random.uniform(min_cost, max_cost), 2)
        prev_arr_date = date + duration
        
        f = generate_flight(i+1, dep, arr, date, duration, cost)
        print(f)
        duration_sum_temp += duration
        route_flights.append(f)
        D = (T - C - duration)/(d - i + 1)
        edate = date + duration
    return route_flights
    
    
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
if len(sys.argv) < 12:
    print("usage: python data_generator.py "
          "\n   <number of flights>"
          "\n   <number of airports>"
          "\n   <number of destinations>"
          "\n   <holiday time>"
          "\n   <min_flight_cost>"
          "\n   <max_flight_cost>"
          "\n   <min_duration>"
          "\n   <max_duration>"
          "\n   <min_connection_time>"
          "\n   <max_connection_time>"
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
min_dur = 0.1 #float(sys.argv[7])
# maximum flight duration:
max_dur = float(sys.argv[8])
# minimum connection time
min_conn = 0 # float(sys.argv[9])
# maximum connection time
max_conn = 0 # float(sys.argv[10])
# output file id
id = sys.argv[11]

if m <= n:
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
