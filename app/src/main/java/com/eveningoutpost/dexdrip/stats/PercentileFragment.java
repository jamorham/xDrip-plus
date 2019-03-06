package com.eveningoutpost.dexdrip.stats;

import android.os.*;
import android.view.*;

import androidx.annotation.*;
import androidx.fragment.app.*;

import com.eveningoutpost.dexdrip.models.*;
import com.eveningoutpost.dexdrip.models.UserError.*;

/**
 * Created by adrian on 30/06/15.
 */
public class PercentileFragment extends Fragment {

    private PercentileView percentileView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        UserError.Log.i("DrawStats", "PercentileFragment - onCreateView");
        return getView();
    }

    @Nullable
    @Override
    public View getView() {
        UserError.Log.i("DrawStats", "PercentileFragment - getView");

        if (percentileView == null) {
            percentileView = new PercentileView(getActivity().getApplicationContext());
            percentileView.setTag(2);
        }
        return percentileView;
    }
}
