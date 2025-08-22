import discord
from discord import app_commands
import re
import logging
from db import create_tables, register_user, get_user
import config

# Set up logging
logging.basicConfig(level=logging.INFO)

intents = discord.Intents.default()
intents.members = True

bot = discord.Client(intents=intents)
tree = app_commands.CommandTree(bot)

ADMIN_ROLE_ID = config.ADMIN_ROLE_ID
MILCOM_ROLE_ID = config.MILCOM_ROLE_ID
DISCORD_TOKEN = config.DISCORD_TOKEN

# --- Helpers ---
def get_nation_id_from_link(link: str):
    match = re.search(r'nation/id=(\d+)', link)
    if match:
        return int(match.group(1))
    return None

# --- Permission Checks ---
def is_admin(interaction: discord.Interaction) -> bool:
    return any(role.id == ADMIN_ROLE_ID for role in interaction.user.roles)

def is_milcom(interaction: discord.Interaction) -> bool:
    return any(role.id in (ADMIN_ROLE_ID, MILCOM_ROLE_ID) for role in interaction.user.roles)

def admin_check():
    async def predicate(interaction: discord.Interaction) -> bool:
        return is_admin(interaction)
    return app_commands.check(predicate)

def milcom_check():
    async def predicate(interaction: discord.Interaction) -> bool:
        return is_milcom(interaction)
    return app_commands.check(predicate)

# --- Error Handler for Permission Checks ---
@tree.error
async def on_app_command_error(interaction: discord.Interaction, error: app_commands.AppCommandError):
    if isinstance(error, app_commands.CheckFailure):
        await interaction.response.send_message("You do not have permission to use this command.", ephemeral=True)
    else:
        logging.error(f"App command error: {error}")
        try:
            await interaction.response.send_message("An error occurred while executing this command.", ephemeral=True)
        except discord.InteractionResponded:
            pass

# --- Events ---
@bot.event
async def on_ready():
    create_tables()
    try:
        synced = await tree.sync()
        logging.info(f"Synced {len(synced)} slash commands")
    except Exception as e:
        logging.error(f"Failed to sync commands: {e}")
    logging.info(f'Bot connected as {bot.user}')

# --- Registration Commands ---
@tree.command(name="register", description="Register your nation")
@app_commands.describe(nation_link="Your Politics & War nation link")
async def register(interaction: discord.Interaction, nation_link: str):
    nation_id = get_nation_id_from_link(nation_link)
    if not nation_id:
        await interaction.response.send_message(
            'Invalid nation link. Please use: https://politicsandwar.com/nation/id=123456',
            ephemeral=True
        )
        return
    leader_name = interaction.user.display_name.split('\\')[0].strip()
    register_user(interaction.user.id, nation_id, leader_name)
    try:
        new_nick = f"{leader_name} \\ {nation_id}"
        await interaction.user.edit(nick=new_nick)
    except Exception as e:
        logging.warning(f'Could not update nickname: {e}')
    await interaction.response.send_message(f'Registered nation {nation_id} for {interaction.user.mention}.')

@tree.command(name="admin_register", description="Register a nation for a member (Admin only)")
@admin_check()
@app_commands.describe(member="The Discord member", nation_link="Nation link")
async def admin_register(interaction: discord.Interaction, member: discord.Member, nation_link: str):
    nation_id = get_nation_id_from_link(nation_link)
    if not nation_id:
        await interaction.response.send_message(
            'Invalid nation link. Please use: https://politicsandwar.com/nation/id=123456',
            ephemeral=True
        )
        return
    leader_name = member.display_name.split('\\')[0].strip()
    register_user(member.id, nation_id, leader_name, registered_by_admin=True)
    try:
        new_nick = f"{leader_name} \\ {nation_id}"
        await member.edit(nick=new_nick)
    except Exception as e:
        logging.warning(f'Could not update nickname: {e}')
    await interaction.response.send_message(f'Admin registered nation {nation_id} for {member.mention}.')

# --- Milcom Commands ---
@tree.command(name="war", description="Find enemy war targets")
@milcom_check()
@app_commands.describe(num_results="How many results to show (default 8)")
async def war(interaction: discord.Interaction, num_results: int = 8):
    from db import get_user, get_nation_by_id, get_alliance_nations
    user = get_user(interaction.user.id)
    if not user:
        await interaction.response.send_message('You must register your nation first using /register.', ephemeral=True)
        return
    nation_id = user['nation_id']
    nation = get_nation_by_id(nation_id)
    if not nation:
        await interaction.response.send_message('Could not fetch your nation data from P&W API.', ephemeral=True)
        return
    alliance_id = nation.get('allianceid')
    if not alliance_id:
        await interaction.response.send_message('You must be in an alliance to use /war.', ephemeral=True)
        return

    alliance_nations = get_alliance_nations(alliance_id)
    all_targets = []
    for aid in range(1, 11):
        if aid == alliance_id:
            continue
        all_targets += get_alliance_nations(aid)

    my_score = float(nation['score'])
    min_score = my_score * 0.75
    max_score = my_score * 1.75
    filtered = []
    for t in all_targets:
        try:
            score = float(t['score'])
            if min_score <= score <= max_score:
                filtered.append(t)
        except Exception:
            continue

    filtered = sorted(filtered, key=lambda n: int(n.get('soldiers', 0)))

    msg = f"**Results for {nation['nationname']}**\n"
    for count, t in enumerate(filtered[:num_results], 1):
        url = f"https://politicsandwar.com/nation/id={t['id']}"
        msg += f"<{url}> | {t['nationname']} | {t['alliancename']} | Score: {t['score']} | Cities: {t['cities']} | Soldiers: {t['soldiers']}\n"
    if not filtered:
        msg += "No targets found."
    await interaction.response.send_message(msg)

@tree.command(name="counter", description="Find counter options for an enemy nation")
@milcom_check()
@app_commands.describe(enemy_id="Enemy nation ID")
async def counter(interaction: discord.Interaction, enemy_id: int):
    from db import get_user, get_nation_by_id, get_alliance_nations
    user = get_user(interaction.user.id)
    if not user:
        await interaction.response.send_message('You must register your nation first using /register.', ephemeral=True)
        return
    nation_id = user['nation_id']
    nation = get_nation_by_id(nation_id)
    if not nation:
        await interaction.response.send_message('Could not fetch your nation data.', ephemeral=True)
        return
    enemy = get_nation_by_id(enemy_id)
    if not enemy:
        await interaction.response.send_message('Could not fetch enemy nation data.', ephemeral=True)
        return
    alliance_id = nation.get('allianceid')
    if not alliance_id:
        await interaction.response.send_message('You must be in an alliance to use /counter.', ephemeral=True)
        return

    counter_nations = get_alliance_nations(alliance_id)
    enemy_score = float(enemy['score'])
    min_score = enemy_score * 0.75
    max_score = enemy_score * 1.75
    filtered = []
    for n in counter_nations:
        try:
            score = float(n['score'])
            if min_score <= score <= max_score and int(n['id']) != nation_id:
                filtered.append(n)
        except Exception:
            continue
    filtered = sorted(filtered, key=lambda n: -int(n.get('soldiers', 0)))

    msg = f"**Counter options for enemy {enemy['nationname']}**\n"
    for count, n in enumerate(filtered[:10], 1):
        url = f"https://politicsandwar.com/nation/id={n['id']}"
        msg += f"<{url}> | {n['nationname']} | Score: {n['score']} | Cities: {n['cities']} | Soldiers: {n['soldiers']}\n"
    if not filtered:
        msg += "No counters found."
    await interaction.response.send_message(msg)

@tree.command(name="spy", description="Calculate spy odds on a target nation")
@milcom_check()
@app_commands.describe(target_id="Target nation ID", spies_used="How many spies to send", safety="quick, normal, or covert")
async def spy(interaction: discord.Interaction, target_id: int, spies_used: int = 60, safety: str = "normal"):
    from db import get_user, get_nation_by_id
    user = get_user(interaction.user.id)
    if not user:
        await interaction.response.send_message('You must register your nation first using /register.', ephemeral=True)
        return
    nation_id = user['nation_id']
    me = get_nation_by_id(nation_id)
    if not me:
        await interaction.response.send_message('Could not fetch your nation data.', ephemeral=True)
        return
    target = get_nation_by_id(target_id)
    if not target:
        await interaction.response.send_message('Could not fetch target nation data.', ephemeral=True)
        return

    safety = safety.lower()
    if safety not in ['quick', 'normal', 'covert']:
        await interaction.response.send_message('Safety must be one of: quick, normal, covert.', ephemeral=True)
        return

    try:
        target_spies = int(target.get('spies', 1))
    except Exception:
        target_spies = 1
    base_odds = min(95, int((spies_used / (target_spies+1)) * 60))
    if safety == 'quick':
        odds = int(base_odds * 0.7)
    elif safety == 'covert':
        odds = int(base_odds * 0.5)
    else:
        odds = int(base_odds)

    msg = f"Spy odds for {target['nationname']} (spies used: {spies_used}, safety: {safety}):\n"
    msg += f"Estimated odds: {odds}%\n"
    msg += f"Target spies: {target.get('spies','?')} | Your spies: {me.get('spies','?')}\n"
    await interaction.response.send_message(msg)

# --- Alliance Commands (placeholders) ---
@tree.command(name="alliance_revenue", description="Alliance revenue (placeholder)")
@milcom_check()
async def alliance_revenue(interaction: discord.Interaction):
    await interaction.response.send_message('/alliance revenue command placeholder.')

@tree.command(name="alliance_cost", description="Alliance cost (placeholder)")
@milcom_check()
async def alliance_cost(interaction: discord.Interaction):
    await interaction.response.send_message('/alliance cost command placeholder.')

@tree.command(name="alliance_departures", description="Alliance departures (placeholder)")
@milcom_check()
async def alliance_departures(interaction: discord.Interaction):
    await interaction.response.send_message('/alliance departures command placeholder.')

@tree.command(name="alliance_markasoffshore", description="Alliance mark as offshore (placeholder)")
@milcom_check()
async def alliance_markasoffshore(interaction: discord.Interaction):
    await interaction.response.send_message('/alliance markasoffshore command placeholder.')

if __name__ == '__main__':
    bot.run(DISCORD_TOKEN)
