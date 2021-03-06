package com.podevs.android.poAndroid.poke;

import java.util.LinkedList;

import android.text.Html;
import android.text.SpannableStringBuilder;

import com.podevs.android.poAndroid.battle.BattleMove;
import com.podevs.android.poAndroid.poke.PokeEnums.Status;
import com.podevs.android.poAndroid.pokeinfo.PokemonInfo;
import com.podevs.android.poAndroid.pokeinfo.TypeInfo.Type;
import com.podevs.android.utilities.Bais;
import com.podevs.android.utilities.Baos;
import com.podevs.android.utilities.SerializeBytes;

// This class represents the Opponent's poke during a battle.
public class ShallowBattlePoke implements SerializeBytes {
	public String rnick, nick = "", pokeName = "";
	int fullStatus = 0;
	public UniqueID uID = new UniqueID();
	public Type[] types = new Type[2];
	public boolean shiny = false;
	public byte gender = 0;
	public byte lifePercent = 0;
	public byte level = 0;
	public byte lastKnownPercent = 0;
	public boolean sub = false;
	public LinkedList<UniqueID> specialSprites = new LinkedList<UniqueID>();
	public BattleMove[] moves = new BattleMove[4];
	public Integer[][] stats = new Integer[2][6];

	public ShallowBattlePoke() {}; // For pokes who have not been sent out;
	
	public ShallowBattlePoke(Bais msg, boolean isMe, Gen gen) {
		uID = new UniqueID(msg);
		rnick = nick = msg.readString();
		if (!isMe) {
			nick = "the foe's " + nick;
			
			// A little optimization; these only matter if it's not your poke
			pokeName = PokemonInfo.name(uID);
			types[0] = Type.values()[PokemonInfo.type1(uID, gen.num)];
			types[1] = Type.values()[PokemonInfo.type2(uID, gen.num)];
		}
		lifePercent = msg.readByte();
		fullStatus = msg.readInt();
		gender = msg.readByte();
		shiny = msg.readBool();
		level = msg.readByte();
		if (!isMe) {
			for (int j = 0; j < 2; j++) {
				for (int i = 0; i < 6; i++) {
					stats[j][i] = PokemonInfo.calcMinMaxStat(uID, i, gen.num, level, j);
				}
			}
		}
	}

	public void serializeBytes(Baos b) {
		b.putBaos(uID);
		b.putString(nick);
		b.write(lifePercent);
		b.putInt(fullStatus);
		b.write(gender);
		b.putBool(shiny);
		b.write(level);
	}
	
	public SpannableStringBuilder nameAndType() {
		SpannableStringBuilder s = new SpannableStringBuilder(Html.fromHtml("<b>" + pokeName + "</b>"));
		s.append("\n" + types[0]);
		if(types[1] != Type.Curse) s.append("/" + types[1]);
		return s;
	}

	public void changeStatus(byte status) {
		/* Clears past status */
		fullStatus = fullStatus & ~( (1 << Status.Koed.poValue()) | 0x3F);
		/* Adds new status */
		fullStatus = fullStatus | ( 1 << status);
	}
	
	public final int status() {
		if ((fullStatus & (1 << Status.Koed.poValue())) != 0)
			return Status.Koed.poValue();
		// intlog2(fullStatus & 0x3F)
		int x = fullStatus & 0x3F;
		int i;
		for (i = 0; x > 1; i++) {
			x/=2;
		}
		return i;
	}

	public void addMove(Short attack) {
		if (this.moves[3] == null) {
			for (int i = 0; i < 5; i++) {
				if (this.moves[i] == null) {
					BattleMove newMove = new BattleMove(attack);
					newMove.currentPP = (byte) (newMove.totalPP - 1);
					this.moves[i] = newMove;
					break;
				} else if (this.moves[i].num == attack) {
					this.moves[i].currentPP = (byte) (this.moves[i].currentPP - 1);
					break;
				}
			}
		}
	}

	public String movesString() {
		String s = "";
		for (int i = 0; i < 4; i++) {
			s += (i == 0 ? "" : "\n");
			if (this.moves[i] == null) {
				s += "????" + "    " +"??/??";
			} else {
				s += moves[i].toString() + "    " + moves[i].stringPP();
			}
		}
		return s;
	}

	public String statString(byte[] boostList) {
		String s = "";
		s += (int)(stats[0][0]*lifePercent)/100 + "/" + stats[0][0] + "-" + (int)(stats[1][0]*lifePercent)/100 + "/" + stats[1][0];
		for (int i = 1; i < 6; i++) {
			double Percent = boostPercent(boostList[i-1]);
			s += (i == 0 ? "" : "\n") + (int)(stats[0][i]*Percent) + "-" + (int)(stats[1][i]*Percent);
		}
		return s;
	}

	private double boostPercent(double b) {
		if (b > 0) {
			b = b + 2;
			return b/(2.000);
		} else if (b < 0) {
			b = (-1*b) + 2;
			return (2.000)/b;
		} else {
			return 1;
		}
	}
}
