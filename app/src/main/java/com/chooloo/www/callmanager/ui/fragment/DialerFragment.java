package com.chooloo.www.callmanager.ui.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProviders;

import com.chooloo.www.callmanager.R;
import com.chooloo.www.callmanager.listener.AllPurposeTouchListener;
import com.chooloo.www.callmanager.ui.activity.MainActivity;
import com.chooloo.www.callmanager.ui.fragment.base.BaseFragment;
import com.chooloo.www.callmanager.util.CallManager;
import com.chooloo.www.callmanager.util.Utilities;
import com.google.i18n.phonenumbers.AsYouTypeFormatter;
import com.google.i18n.phonenumbers.PhoneNumberUtil;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnLongClick;

public class DialerFragment extends BaseFragment {

    public static final String ARG_DIALER = "dialer";

    private SharedDialViewModel mViewModel;
    private String mNumberText = "";
    private AsYouTypeFormatter mAsYouTypeFormatter;

    private boolean mIsDialer = true;

    // Edit Texts
    @BindView(R.id.text_number_input) TextView mNumberView;

    // Buttons
    @BindView(R.id.button_call) ImageView mCallButton;
    @BindView(R.id.button_delete) ImageView mDelButton;
    @BindView(R.id.chip0) TextView mChip0;

    // Layouts
    @BindView(R.id.table_numbers) TableLayout mNumbersTable;

    public static DialerFragment newInstance(boolean isDialer) {
        Bundle args = new Bundle();
        args.putBoolean(ARG_DIALER, isDialer);
        DialerFragment fragment = new DialerFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreateView() {
        Bundle args = getArguments();
        if (args == null)
            throw new IllegalArgumentException("You must create this fragment with newInstance()");
        mIsDialer = args.getBoolean(ARG_DIALER);

        if (!mIsDialer) {
            mCallButton.setVisibility(View.GONE);
            mDelButton.setVisibility(View.GONE);
            mNumberView.setHint("");
        } else {
            AllPurposeTouchListener swipeToDelListener = new AllPurposeTouchListener(getContext()) {
                @Override
                public void onSwipeLeft() {
                    delNum(mDelButton);
                }

                @Override
                public boolean onSingleTapUp(View v) {
                    ((MainActivity) DialerFragment.this.getActivity()).expandDialer(true);
                    return true;
                }
            };
            mNumberView.setOnTouchListener(swipeToDelListener);
        }
    }

    @Override
    protected int layoutId() {
        return R.layout.fragment_dial;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(getActivity()).get(SharedDialViewModel.class);
        mViewModel.getNumber().observe(this, s -> {
            if (!s.equals(mNumberText)) {
                setNumber(s, true);
            }
        });

        mAsYouTypeFormatter = PhoneNumberUtil.getInstance().getAsYouTypeFormatter(Utilities.sLocale.getCountry());
    }

    // -- On Clicks -- //

    /**
     * Dialer buttons click handler
     *
     * @param view is the button number
     */
    @OnClick({R.id.chip0, R.id.chip1, R.id.chip2, R.id.chip3, R.id.chip4, R.id.chip5,
            R.id.chip6, R.id.chip7, R.id.chip8, R.id.chip9, R.id.chip_star, R.id.chip_hex})
    public void addChar(View view) {
        vibrate();
        String id = getResources().getResourceEntryName(view.getId());
        char toAdd;
        if (id.contains("chip_star")) toAdd = '*';
        else if (id.contains("chip_hex")) toAdd = '#';
        else toAdd = id.charAt(4);

        setNumber(inputDigit(toAdd));
    }

    /**
     * Deletes a number from the keypad's input when the delete button is clicked
     */
    @OnClick(R.id.button_delete)
    public void delNum(View view) {
        if (mNumberText.length() <= 0) return;
        vibrate();
        mNumberText = mNumberText.substring(0, mNumberText.length() - 1);

        if (mIsDialer) { // Rebuild the formatter
            mAsYouTypeFormatter.clear();

            for (int i = 0; i < mNumberText.length(); i++) {
                String s = mAsYouTypeFormatter.inputDigit(mNumberText.charAt(i));
                if (i == mNumberText.length() - 1) setNumber(s);
            }
        }

        if (mNumberText.length() == 0) {
            delAllNum(view);
        }
    }

    /**
     * Deletes the whole keypad's input when the delete button is long clicked
     */
    @OnLongClick(R.id.button_delete)
    public boolean delAllNum(View view) {
        mNumberText = "";
        mAsYouTypeFormatter.clear();
        setNumber("");
        return true;
    }

    /**
     * Starts a call to voice mail when the 1 button is long clicked
     */
    @OnLongClick(R.id.chip1)
    public boolean startVoiceMail(View view) {
        if (!mIsDialer) return false;
        return CallManager.callVoicemail(this.getContext());
    }

    @OnLongClick(R.id.chip0)
    public boolean addPlus(View view) {
        if (!mIsDialer) return false;
        mNumberText += "+";
        setNumber(inputDigit('+'));
        return true;
    }

    /**
     * Calls the number in the keypad's input
     */
    @OnClick(R.id.button_call)
    public void call(View view) {
        CallManager.call(this.getContext(), mNumberText);
    }

    // -- Setters -- //

    private String inputDigit(char c) {
        mNumberText += c;
        if (mIsDialer)
            return mAsYouTypeFormatter.inputDigit(c);
        else
            return mNumberText;
    }

    public void setNumber(String number, boolean updateAll) {
        if (updateAll) {
            mNumberText = number;
            mNumberView.setText(number);
            mAsYouTypeFormatter.clear();
            for (int i = 0; i < number.length(); i++) {
                mAsYouTypeFormatter.inputDigit(number.charAt(i));
            }

            mViewModel.setNumber(mNumberText);
        } else {
            setNumber(number);
        }
    }

    /**
     * Sets the input number to a given number
     * This function for now will be called due to list item click (contact)
     *
     * @param number
     */
    public void setNumber(String number) {
        mNumberView.setText(number);
        mViewModel.setNumber(mNumberText);
    }

    // -- Utils -- //

    private void vibrate() {
        Utilities.vibrate(getContext(), Utilities.SHORT_VIBRATE_LENGTH);
    }


}
