package ml.pixreward.app;

import android.widget.ListAdapter;
import com.google.firebase.database.IgnoreExtraProperties;
import java.util.List;

@IgnoreExtraProperties
public class Ranking {
    private String uid;
    private String name;
	private Integer points;

    public Ranking() {

    }

    public Ranking(String uid, String name, Integer points) {
        this.uid = uid;
        this.name = name;
		this.points = points;
    }

    public String getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }

	public Integer getPoints() {
		return points;
	}


}
