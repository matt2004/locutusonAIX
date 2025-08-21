import mysql.connector
from mysql.connector import Error

from config import DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASS

def get_connection():
    """
    Establishes a connection to the MySQL database.
    """
    return mysql.connector.connect(
        host=DB_HOST,
        port=DB_PORT,
        database=DB_NAME,
        user=DB_USER,
        password=DB_PASS
    )

# Minimal schema for MVP
# - users: discord_id (PK), nation_id, leader_name, alliance_id, registered_by_admin (bool)
# - alliances: alliance_id (PK), name, offshore (bool)

def create_tables():
    """
    Creates the users and alliances tables in the database.
    """
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute('''
    CREATE TABLE IF NOT EXISTS users (
        discord_id BIGINT PRIMARY KEY,
        nation_id INT NOT NULL,
        leader_name VARCHAR(100) NOT NULL,
        alliance_id INT,
        registered_by_admin BOOLEAN DEFAULT FALSE
    ) ENGINE=InnoDB;
    ''')
    cursor.execute('''
    CREATE TABLE IF NOT EXISTS alliances (
        alliance_id INT PRIMARY KEY,
        name VARCHAR(100) NOT NULL,
        offshore BOOLEAN DEFAULT FALSE
    ) ENGINE=InnoDB;
    ''')
    conn.commit()
    cursor.close()
    conn.close()

# Example user functions
import requests

def get_nation_by_id(nation_id):
    # Fetch a nation from the P&W API
    url = f"https://api.politicsandwar.com/graphql?query={{nation(id:{nation_id}){id leadername nationname allianceid alliancename score cities soldiers tanks aircraft ships defcon spies missiles nukes vmode beige_turns activity}}}"
    resp = requests.get(url)
    if resp.status_code != 200:
        return None
    data = resp.json().get('data', {}).get('nation')
    return data

def get_alliance_nations(alliance_id):
    # Fetch all nations in an alliance from the P&W API
    url = f"https://api.politicsandwar.com/graphql?query={{nations(alliance_id:{alliance_id}){id leadername nationname score cities soldiers tanks aircraft ships defcon spies missiles nukes vmode beige_turns activity}}}"
    resp = requests.get(url)
    if resp.status_code != 200:
        return []
    data = resp.json().get('data', {}).get('nations', [])
    return data

def get_active_wars(nation_id):
    # Fetch active wars for a nation from the P&W API
    url = f"https://api.politicsandwar.com/graphql?query={{wars(nation_id:{nation_id},status:active){id attacker_id defender_id status}}}"
    resp = requests.get(url)
    if resp.status_code != 200:
        return []
    data = resp.json().get('data', {}).get('wars', [])
    return data

def register_user(discord_id, nation_id, leader_name, alliance_id=None, registered_by_admin=False):
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute(
        "REPLACE INTO users (discord_id, nation_id, leader_name, alliance_id, registered_by_admin) VALUES (%s, %s, %s, %s, %s)",
        (discord_id, nation_id, leader_name, alliance_id, registered_by_admin)
    )
    conn.commit()
    cursor.close()
    conn.close()

def get_user(discord_id):
    conn = get_connection()
    cursor = conn.cursor(dictionary=True)
    cursor.execute("SELECT * FROM users WHERE discord_id = %s", (discord_id,))
    user = cursor.fetchone()
    cursor.close()
    conn.close()
    return user