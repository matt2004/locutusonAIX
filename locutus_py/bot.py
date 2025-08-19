import discord
from discord.ext import commands
import re
import logging
from db import create_tables, register_user, get_user
import config

# Set up logging
logging.basicConfig(level=logging.INFO)

intents = discord.Intents.default()
intents.members = True

bot = commands.Bot(command_prefix='/', intents=intents)

ADMIN_ROLE_ID = config.ADMIN_ROLE_ID
MILCOM_ROLE_ID = config.MILCOM_ROLE_ID

# --- Helpers ---
def get_nation_id_from_link(link: str):
    match = re.search(r'nation/id=(\d+)', link)
    if match:
        return int(match.group(1))
    return None

# --- Permission Checks ---
def is_admin():
    async def predicate(ctx):
        return any(role.id == ADMIN_ROLE_ID for role in ctx.author.roles)
    return commands.check(predicate)

def is_milcom():
    async def predicate(ctx):
        return any(role.id in (ADMIN_ROLE_ID, MILCOM_ROLE_ID) for role in ctx.author.roles)
    return commands.check(predicate)

# --- Events ---
@bot.event
async def on_ready():
    create_tables()
    logging.info(f'Bot connected as {bot.user}')

# --- Registration Commands ---
@bot.command()
async def register(ctx, nation_link: str):
    nation_id = get_nation_id_from_link(nation_link)
    if not nation_id:
        await ctx.send('Invalid nation link. Please use: https://politicsandwar.com/nation/id=123456')
        return
    leader_name = ctx.author.display_name.split('\\')[0].strip()
    register_user(ctx.author.id, nation_id, leader_name)
    # Update nickname
    try:
        new_nick = f"{leader_name} \\ {nation_id}"
        await ctx.author.edit(nick=new_nick)
    except Exception as e:
        logging.warning(f'Could not update nickname: {e}')
    await ctx.send(f'Registered nation {nation_id} for {ctx.author.mention}.')

@bot.command()
@is_admin()
async def admin_register(ctx, member: discord.Member, nation_link: str):
    nation_id = get_nation_id_from_link(nation_link)
    if not nation_id:
        await ctx.send('Invalid nation link. Please use: https://politicsandwar.com/nation/id=123456')
        return
    leader_name = member.display_name.split('\\')[0].strip()
    register_user(member.id, nation_id, leader_name, registered_by_admin=True)
    try:
        new_nick = f"{leader_name} \\ {nation_id}"
        await member.edit(nick=new_nick)
    except Exception as e:
        logging.warning(f'Could not update nickname: {e}')
    await ctx.send(f'Admin registered nation {nation_id} for {member.mention}.')

# --- Milcom Commands ---
@bot.command()
@is_milcom()
async def war(ctx, *args):
    from db import get_user, get_nation_by_id, get_alliance_nations
    user = get_user(ctx.author.id)
    if not user:
        await ctx.send('You must register your nation first using /register.')
        return
    nation_id = user['nation_id']
    nation = get_nation_by_id(nation_id)
    if not nation:
        await ctx.send('Could not fetch your nation data from P&W API.')
        return
    alliance_id = nation.get('allianceid')
    if not alliance_id:
        await ctx.send('You must be in an alliance to use /war.')
        return
    # Parse args for options (simple MVP: just number of results)
    num_results = 8
    for arg in args:
        if arg.isdigit():
            num_results = int(arg)
    # Get all alliance nations (as possible attackers)
    alliance_nations = get_alliance_nations(alliance_id)
    # Get all possible targets (enemies = not in your alliance)
    # For MVP, get all nations and filter by not in your alliance
    all_targets = []
    for aid in range(1, 11):  # Only check first 10 alliances for MVP
        if aid == alliance_id:
            continue
        all_targets += get_alliance_nations(aid)
    # Filter by war range (score 0.75x to 1.75x)
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
    # Sort by lowest soldiers (easiest target approximation)
    filtered = sorted(filtered, key=lambda n: int(n.get('soldiers', 0)))
    # Format output
    msg = f"**Results for {nation['nationname']}**\n"
    count = 0
    for t in filtered:
        if count >= num_results:
            break
        url = f"https://politicsandwar.com/nation/id={t['id']}"
        msg += f"<{url}> | {t['nationname']} | {t['alliancename']} | Score: {t['score']} | Cities: {t['cities']} | Soldiers: {t['soldiers']}\n"
        count += 1
    if count == 0:
        msg += "No targets found."
    await ctx.send(msg)

@bot.command()
@is_milcom()
async def counter(ctx, *args):
    from db import get_user, get_nation_by_id, get_alliance_nations
    user = get_user(ctx.author.id)
    if not user:
        await ctx.send('You must register your nation first using /register.')
        return
    nation_id = user['nation_id']
    nation = get_nation_by_id(nation_id)
    if not nation:
        await ctx.send('Could not fetch your nation data from P&W API.')
        return
    if not args:
        await ctx.send('Usage: /counter <enemy_nation_id>')
        return
    try:
        enemy_id = int(args[0])
    except Exception:
        await ctx.send('First argument must be the enemy nation ID.')
        return
    enemy = get_nation_by_id(enemy_id)
    if not enemy:
        await ctx.send('Could not fetch enemy nation data.')
        return
    alliance_id = nation.get('allianceid')
    if not alliance_id:
        await ctx.send('You must be in an alliance to use /counter.')
        return
    # Get all alliance nations (as possible counters)
    counter_nations = get_alliance_nations(alliance_id)
    # Filter by war range (score 0.75x to 1.75x of enemy)
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
    # Sort by highest soldiers (strongest first)
    filtered = sorted(filtered, key=lambda n: -int(n.get('soldiers', 0)))
    msg = f"**Counter options for enemy {enemy['nationname']}**\n"
    count = 0
    for n in filtered:
        if count >= 10:
            break
        url = f"https://politicsandwar.com/nation/id={n['id']}"
        msg += f"<{url}> | {n['nationname']} | Score: {n['score']} | Cities: {n['cities']} | Soldiers: {n['soldiers']}\n"
        count += 1
    if count == 0:
        msg += "No counters found."
    await ctx.send(msg)

@bot.command()
@is_milcom()
async def spy(ctx, *args):
    from db import get_user, get_nation_by_id
    user = get_user(ctx.author.id)
    if not user:
        await ctx.send('You must register your nation first using /register.')
        return
    nation_id = user['nation_id']
    me = get_nation_by_id(nation_id)
    if not me:
        await ctx.send('Could not fetch your nation data from P&W API.')
        return
    if not args:
        await ctx.send('Usage: /spy <target_nation_id> [spies_used] [safety]')
        return
    try:
        target_id = int(args[0])
    except Exception:
        await ctx.send('First argument must be the target nation ID.')
        return
    target = get_nation_by_id(target_id)
    if not target:
        await ctx.send('Could not fetch target nation data.')
        return
    spies_used = 60
    if len(args) >= 2:
        try:
            spies_used = int(args[1])
        except Exception:
            await ctx.send('Second argument (spies_used) must be an integer.')
            return
    safety = 'normal'
    if len(args) >= 3:
        safety = args[2].lower()
        if safety not in ['quick', 'normal', 'covert']:
            await ctx.send('Safety must be one of: quick, normal, covert.')
            return
    # Dummy odds calculation for MVP (replace with real P&W odds logic if needed)
    # Odds formula: base = spies_used / (target['spies']+1), mod by safety
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
    await ctx.send(msg)

# --- Alliance Commands ---
@bot.command()
@is_milcom()
async def alliance_revenue(ctx, *args):
    await ctx.send('/alliance revenue command placeholder. (To be implemented)')

@bot.command()
@is_milcom()
async def alliance_cost(ctx, *args):
    await ctx.send('/alliance cost command placeholder. (To be implemented)')

@bot.command()
@is_milcom()
async def alliance_departures(ctx, *args):
    await ctx.send('/alliance departures command placeholder. (To be implemented)')

@bot.command()
@is_milcom()
async def alliance_markasoffshore(ctx, *args):
    await ctx.send('/alliance markasoffshore command placeholder. (To be implemented)')

if __name__ == '__main__':
    bot.run(config.DISCORD_TOKEN)
