package biz.dealnote.messenger.dialog.base;

import androidx.fragment.app.DialogFragment;

import biz.dealnote.messenger.util.ViewUtils;

public abstract class BaseDialogFragment extends DialogFragment {

    @Override
    public void onDestroy() {
        super.onDestroy();
        ViewUtils.keyboardHide(requireActivity());
    }
}