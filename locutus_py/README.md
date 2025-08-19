# Locutus Python Rewrite

This folder contains the new Python implementation of the Locutus Discord bot for Politics and War, focusing on the core functionality:
- Nation registration (user/admin flows)
- Milcom commands (/war, /counter, /spy)
- Alliance commands (/alliance revenue, /alliance cost, /alliance departures, /alliance markasoffshore)
- Role-based access control (Admin, Milcom, User)
- MySQL/MariaDB backend

## Structure
- `bot.py`: Main bot entry point
- `db.py`: Database models and access
- `cogs/`: Discord command modules (registration, milcom, alliance)
- `config.py`: Configuration (token, DB, role IDs)
- `requirements.txt`: Dependencies
- `README.md`: This file
- `docs/`: Quick docs for stripped Java functions

## Setup
1. Install requirements: `pip install -r requirements.txt`
2. Configure `config.py` for your bot token and DB credentials
3. Run the bot: `python bot.py`

## Systemd Example
See `docs/systemd-example.service`

---

For further migration or feature expansion, see `docs/stripped_java_functions.md` for documentation on omitted Java logic.
