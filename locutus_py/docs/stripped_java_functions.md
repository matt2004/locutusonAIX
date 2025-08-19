# Quick docs for stripped Java functions (excluding web app)
# This file will be filled as part of the migration process.

# --- Registration Logic ---
# Java: RegisterCommand.java, DiscordCommands.java
# - /register <nation_link>: Links a Discord user to a Politics & War nation ID.
# - /admin register @user <nation_link>: Admin can link any Discord user to a nation.
# - Nickname is set to "Leader Name \ NationID" after registration.
# - Minimal validation in Python version (no in-game username checks).
#
# --- Milcom Logic ---
# Java: Commands beginning with /war, /counter, /spy (see DiscordCommands.java and related)
# - Used for military coordination (details to be re-implemented as needed).
#
# --- Alliance Logic ---
# Java: /alliance revenue, /alliance cost, /alliance departures, /alliance markasoffshore (see AllianceMetricCommands.java)
# - Alliance metrics, cost, revenue, departures, and marking as offshore.
# - Dependencies and logic to be ported as needed.
#
# --- Permissions ---
# Java: Role-based access control, with Admin, Milcom, and User roles.
# - Python version will use Discord role IDs from config.
#
# --- Database ---
# Java: DBMain.java, DBNation.java, etc.
# - Stores user/nation links and alliance data.
# - Python version will use MySQL/MariaDB with a minimal schema.
