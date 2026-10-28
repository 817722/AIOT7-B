// 지역 연동 필터: 선택한 도/광역시의 시군구만 활성화합니다.
document.addEventListener('DOMContentLoaded', () => {
    const provinceSelect = document.getElementById('bridge-province');
    const citySelect = document.getElementById('bridge-city');
    if (!provinceSelect || !citySelect) return;

    function updateCities() {
        const currentProvince = provinceSelect.value;
        Array.from(citySelect.options).forEach((option) => {
            if (!option.value) return;
            const available = !currentProvince || option.dataset.province === currentProvince;
            option.hidden = !available;
            option.disabled = !available;
        });
        const selected = citySelect.selectedOptions[0];
        if (selected && selected.disabled) citySelect.value = '';
    }

    provinceSelect.addEventListener('change', updateCities);
    updateCities();
});